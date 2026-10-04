package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Equal-weight ridge fit of score margins, with bounded player-count-scaled strength. */
public class TeamStrength {
    private final HashMap<Long, Integer> playerIndices;
    private final double[] effects;

    private TeamStrength(HashMap<Long, Integer> playerIndices, double[] effects) {
        this.playerIndices = playerIndices;
        this.effects = effects;
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

        // Penalty 1 for every player and pair: beta = X' (I + X X')^-1 y.
        // Each game contributes one row, regardless of date, roster size or score total.
        double[][] kernel = new double[history.size()][history.size()];
        for (int i = 0; i < history.size(); i++) {
            for (int j = 0; j <= i; j++) {
                double value = 0;
                for (int f = 0; f < features; f++) {
                    value += rows[i][f] * rows[j][f];
                }
                kernel[i][j] = kernel[j][i] = value;
            }
            kernel[i][i] += 1;
        }
        solve(kernel, margins);
        double[] effects = new double[features];
        for (int g = 0; g < history.size(); g++) {
            for (int f = 0; f < features; f++) {
                effects[f] += rows[g][f] * margins[g];
            }
        }
        return new TeamStrength(indices, effects);
    }

    public double forTeam(List<Player> team) {
        if (team.isEmpty()) {
            return 0;
        }
        double effect = 0;
        for (int i = 0; i < team.size(); i++) {
            Integer first = playerIndices.get(team.get(i).id);
            if (first == null) {
                continue;
            }
            effect += effects[first];
            for (int j = i + 1; j < team.size(); j++) {
                Integer second = playerIndices.get(team.get(j).id);
                if (second != null) {
                    effect += effects[pairIndex(Math.min(first, second),
                            Math.max(first, second), playerIndices.size())];
                }
            }
        }
        double perPlayer = 0.5 + effect / team.size();
        return team.size() * Math.max(0.3, Math.min(0.7, perPlayer));
    }

    private static int pairIndex(int first, int second, int count) {
        return count + first * (2 * count - first - 1) / 2 + second - first - 1;
    }

    // I + X X' is positive definite, including duplicate or contradictory game rows.
    private static void solve(double[][] matrix, double[] values) {
        for (int col = 0; col < values.length; col++) {
            int pivot = col;
            for (int row = col + 1; row < values.length; row++) {
                if (Math.abs(matrix[row][col]) > Math.abs(matrix[pivot][col])) {
                    pivot = row;
                }
            }
            double[] swap = matrix[col];
            matrix[col] = matrix[pivot];
            matrix[pivot] = swap;
            double value = values[col];
            values[col] = values[pivot];
            values[pivot] = value;
            double divisor = matrix[col][col];
            for (int j = col; j < values.length; j++) {
                matrix[col][j] /= divisor;
            }
            values[col] /= divisor;
            for (int row = 0; row < values.length; row++) {
                if (row == col) {
                    continue;
                }
                double factor = matrix[row][col];
                for (int j = col; j < values.length; j++) {
                    matrix[row][j] -= factor * matrix[col][j];
                }
                values[row] -= factor * values[col];
            }
        }
    }
}
