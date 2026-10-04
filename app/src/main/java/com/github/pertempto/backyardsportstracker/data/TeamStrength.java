package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Equal-weight score-margin fit with signed pair effects and shared negative-effect budgets. */
public class TeamStrength {
    private static final double PLAYER_PENALTY = 10;
    private static final double PAIR_PENALTY = 1;
    private static final double NEGATIVE_PAIR_BUDGET = 0.5;
    private static final double FIT_TOLERANCE = 1e-10;
    private final HashMap<Long, Integer> playerIndices;
    private final double[] contributions;

    private TeamStrength(HashMap<Long, Integer> playerIndices, double[] contributions) {
        this.playerIndices = playerIndices;
        this.contributions = contributions;
    }

    public static TeamStrength fromGames(List<Game> games, String sport) {
        List<Game> history = new ArrayList<>();
        HashSet<Long> ids = new HashSet<>();
        for (Game game : games) {
            if (game.deleted || (sport != null && !sport.equals(game.sport))) {
                continue;
            }
            history.add(game);
            for (Player player : game.team1) {
                ids.add(player.id);
            }
            for (Player player : game.team2) {
                ids.add(player.id);
            }
        }
        List<Long> players = new ArrayList<>(ids);
        Collections.sort(players);
        HashMap<Long, Integer> indices = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            indices.put(players.get(i), i);
        }
        int count = players.size();
        int features = count + count * (count - 1) / 2;
        double[][] rows = new double[history.size()][features];
        double[] margins = new double[history.size()];
        for (int g = 0; g < history.size(); g++) {
            Game game = history.get(g);
            for (Player player : game.team1) {
                rows[g][indices.get(player.id)] = 1;
            }
            for (Player player : game.team2) {
                rows[g][indices.get(player.id)] = -1;
            }
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (rows[g][i] == rows[g][j]) {
                        rows[g][pairIndex(i, j, count)] = rows[g][i];
                    }
                }
            }
            margins[g] = (double) game.team1Score - game.team2Score;
        }

        return new TeamStrength(indices, fit(rows, margins, count, features));
    }

    public double forTeam(List<Player> team) {
        double strength = 0;
        for (int i = 0; i < team.size(); i++) {
            Integer first = playerIndices.get(team.get(i).id);
            if (first == null) {
                strength += 1;
                continue;
            }
            strength += contributions[first];
            for (int j = i + 1; j < team.size(); j++) {
                Integer second = playerIndices.get(team.get(j).id);
                if (second != null) {
                    strength += contributions[pairIndex(Math.min(first, second),
                            Math.max(first, second), playerIndices.size())];
                }
            }
        }
        return strength;
    }

    private static int pairIndex(int first, int second, int count) {
        return count + first * (2 * count - first - 1) / 2 + second - first - 1;
    }

    // Every game has equal weight in the regularized score-margin objective.
    // Player weights stay nonnegative; signed pair effects have prior 0.
    // Auxiliary n_ij >= max(0, -b_ij) lets us fit the shared constraint
    // sum_j n_ij <= 0.5*w_i jointly, rather than clipping pair effects after fitting.
    private static double[] fit(double[][] rows, double[] margins, int players, int features) {
        if (features == 0) {
            return new double[0];
        }
        int pairs = features - players;
        int variables = features + pairs;
        double[][] system = new double[variables][variables];
        double[] linear = new double[variables];
        for (int f = 0; f < features; f++) {
            system[f][f] = f < players ? PLAYER_PENALTY : PAIR_PENALTY;
            linear[f] = f < players ? PLAYER_PENALTY : 0;
        }
        for (int g = 0; g < rows.length; g++) {
            for (int f = 0; f < features; f++) {
                linear[f] += rows[g][f] * margins[g];
                for (int j = 0; j <= f; j++) {
                    system[f][j] += rows[g][f] * rows[g][j];
                }
            }
        }

        // Sparse rows of A encode Ax >= 0: w, n, b+n, then each player's shared budget.
        int[][] columns = new int[2 * features][];
        double[][] coefficients = new double[columns.length][];
        int constraint = 0;
        for (int i = 0; i < players; i++) {
            columns[constraint] = new int[]{i};
            coefficients[constraint++] = new double[]{1};
        }
        for (int p = 0; p < pairs; p++) {
            columns[constraint] = new int[]{features + p};
            coefficients[constraint++] = new double[]{1};
        }
        for (int p = 0; p < pairs; p++) {
            columns[constraint] = new int[]{players + p, features + p};
            coefficients[constraint++] = new double[]{1, 1};
        }
        for (int i = 0; i < players; i++) {
            columns[constraint] = new int[players];
            coefficients[constraint] = new double[players];
            columns[constraint][0] = i;
            coefficients[constraint][0] = NEGATIVE_PAIR_BUDGET;
            int k = 1;
            for (int j = 0; j < players; j++) {
                if (i != j) {
                    columns[constraint][k] = features - players
                            + pairIndex(Math.min(i, j), Math.max(i, j), players);
                    coefficients[constraint][k++] = -1;
                }
            }
            constraint++;
        }
        // P + A'A is positive definite: every auxiliary has an identity constraint,
        // and player/pair variables have positive regularization. Factor it once.
        for (int c = 0; c < columns.length; c++) {
            for (int i = 0; i < columns[c].length; i++) {
                for (int j = 0; j <= i; j++) {
                    int first = columns[c][i];
                    int second = columns[c][j];
                    system[Math.max(first, second)][Math.min(first, second)]
                            += coefficients[c][i] * coefficients[c][j];
                }
            }
        }
        factor(system);

        double[] solution = new double[variables];
        Arrays.fill(solution, 0, players, 1);
        double[] projected = new double[columns.length];
        double[] dual = new double[columns.length];
        double[] gradient = new double[variables];
        for (int c = 0; c < columns.length; c++) {
            projected[c] = dot(solution, columns[c], coefficients[c]);
        }
        // Scaled ADMM with penalty 1 and relaxation 1.6. Projection applies to
        // constraint variables only; the returned team strength is never clamped.
        for (int iteration = 0; iteration < 10000; iteration++) {
            System.arraycopy(linear, 0, solution, 0, variables);
            for (int c = 0; c < columns.length; c++) {
                double value = projected[c] - dual[c];
                for (int k = 0; k < columns[c].length; k++) {
                    solution[columns[c][k]] += coefficients[c][k] * value;
                }
            }
            solve(system, solution);
            double primalResidual = 0;
            for (int c = 0; c < columns.length; c++) {
                double value = dot(solution, columns[c], coefficients[c]);
                double relaxed = 1.6 * value - 0.6 * projected[c];
                projected[c] = Math.max(0, relaxed + dual[c]);
                dual[c] += relaxed - projected[c];
                primalResidual = Math.max(primalResidual, Math.abs(value - projected[c]));
            }

            Arrays.fill(gradient, 0);
            for (int f = 0; f < features; f++) {
                double penalty = f < players ? PLAYER_PENALTY : PAIR_PENALTY;
                double prior = f < players ? 1 : 0;
                gradient[f] = penalty * (solution[f] - prior);
            }
            for (int g = 0; g < rows.length; g++) {
                double residual = -margins[g];
                for (int f = 0; f < features; f++) {
                    residual += rows[g][f] * solution[f];
                }
                for (int f = 0; f < features; f++) {
                    gradient[f] += rows[g][f] * residual;
                }
            }
            for (int c = 0; c < columns.length; c++) {
                for (int k = 0; k < columns[c].length; k++) {
                    gradient[columns[c][k]] += coefficients[c][k] * dual[c];
                }
            }
            double dualResidual = 0;
            for (double value : gradient) {
                dualResidual = Math.max(dualResidual, Math.abs(value));
            }
            if (primalResidual <= FIT_TOLERANCE && dualResidual <= FIT_TOLERANCE) {
                return Arrays.copyOf(solution, features);
            }
        }
        // A nonconverged iterate may violate the shared budget. Keep the feasible
        // neutral prior instead of returning an unreliable learned strength.
        double[] neutral = new double[features];
        Arrays.fill(neutral, 0, players, 1);
        return neutral;
    }

    private static double dot(double[] vector, int[] columns, double[] coefficients) {
        double value = 0;
        for (int i = 0; i < columns.length; i++) {
            value += vector[columns[i]] * coefficients[i];
        }
        return value;
    }

    private static void factor(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j <= i; j++) {
                double value = matrix[i][j];
                for (int k = 0; k < j; k++) {
                    value -= matrix[i][k] * matrix[j][k];
                }
                matrix[i][j] = i == j ? Math.sqrt(value) : value / matrix[j][j];
            }
        }
    }

    private static void solve(double[][] factor, double[] values) {
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < i; j++) {
                values[i] -= factor[i][j] * values[j];
            }
            values[i] /= factor[i][i];
        }
        for (int i = values.length - 1; i >= 0; i--) {
            for (int j = i + 1; j < values.length; j++) {
                values[i] -= factor[j][i] * values[j];
            }
            values[i] /= factor[i][i];
        }
    }
}
