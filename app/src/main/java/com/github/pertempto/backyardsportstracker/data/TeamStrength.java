package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Equal-weight score-margin fit with nonnegative player strengths and pair bonuses. */
public class TeamStrength {
    private static final double PLAYER_PENALTY = 10;
    private static final double PAIR_PENALTY = 1;
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

    // Minimize 1/2 sum (Xw - margin)^2 + 1/2 sum penalty * (w - prior)^2, w >= 0.
    // Every historical game has one equally weighted row. Player priors are 1,
    // pair priors are 0; regularization limits sparse-history effects, not team totals.
    // Exact coordinate minimization uses residuals instead of a feature-sized matrix.
    private static double[] fit(double[][] rows, double[] margins, int players, int features) {
        double[] weights = new double[features];
        double[] norms = new double[features];
        double[] residuals = new double[rows.length];
        for (int f = 0; f < features; f++) {
            weights[f] = f < players ? 1 : 0;
            norms[f] = f < players ? PLAYER_PENALTY : PAIR_PENALTY;
            for (double[] row : rows) {
                norms[f] += row[f] * row[f];
            }
        }
        for (int g = 0; g < rows.length; g++) {
            residuals[g] = -margins[g];
            for (int f = 0; f < players; f++) {
                residuals[g] += rows[g][f];
            }
        }
        for (int sweep = 0; sweep < 10000; sweep++) {
            double largestStep = 0;
            for (int f = 0; f < features; f++) {
                double penalty = f < players ? PLAYER_PENALTY : PAIR_PENALTY;
                double prior = f < players ? 1 : 0;
                double gradient = penalty * (weights[f] - prior);
                for (int g = 0; g < rows.length; g++) {
                    gradient += rows[g][f] * residuals[g];
                }
                // Nonnegativity is part of the fitted model, not a clamp on displayed strength.
                double updated = Math.max(0, weights[f] - gradient / norms[f]);
                double step = updated - weights[f];
                weights[f] = updated;
                largestStep = Math.max(largestStep, Math.abs(step));
                for (int g = 0; g < rows.length; g++) {
                    residuals[g] += rows[g][f] * step;
                }
            }
            if (largestStep < 1e-12) {
                break;
            }
        }
        return weights;
    }
}
