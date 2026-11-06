package com.github.pertempto.backyardsportstracker.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Debug-only two-team specialization of openskill.py's Weng-Lin models.
 * Adapted from MIT-licensed openskill.py, Copyright (c) 2024 Vivek Joshy.
 * License included in debug assets/licenses/openskill-python.txt.
 * Score-margin semantics follow revision 65a675d21d9a0d5405731385e83be9aa781537ec.
 * This is not a full OpenSkill port or a claim of IP clearance.
 */
public final class ExperimentalOpenSkill {
    public enum Model { BRADLEY_TERRY, PLACKETT_LUCE }

    public static final class Rating {
        public final double mean;
        public final double sigma;

        public Rating(double mean, double sigma) {
            if (!finite(mean) || !finite(sigma) || sigma <= 0) {
                throw new IllegalArgumentException("Ratings must be finite with positive uncertainty.");
            }
            this.mean = mean;
            this.sigma = sigma;
        }
    }

    private static final double BETA = 25.0 / 6;
    private static final double TAU = 25.0 / 300;

    private ExperimentalOpenSkill() {}

    public static Rating rating(Map<Long, Rating> ratings, long id) {
        Rating value = ratings.get(id);
        return value == null ? new Rating(25, 25.0 / 3) : value;
    }

    public static double forTeam(Map<Long, Rating> ratings, List<Player> team) {
        double mean = 0;
        for (Player player : team) {
            mean += rating(ratings, player.id).mean;
        }
        return mean;
    }

    /**
     * Pure update; callers keep one map per sport. Margin 0 disables margin effects.
     * Weights are explicit positive update multipliers, not inferred teammate effects.
     * They match Python with weight_bounds=None; omitted weights are 1.
     */
    public static Map<Long, Rating> rate(Game game, Map<Long, Rating> prior, Model model,
                                         double margin, Map<Long, Double> weights) {
        validateGame(game);
        if (model == null || !finite(margin) || margin < 0) {
            throw new IllegalArgumentException("Require a model and finite nonnegative margin.");
        }
        List<List<Player>> teams = Arrays.asList(game.team1, game.team2);
        double[] means = new double[2];
        double[] variances = new double[2];
        for (int i = 0; i < 2; i++) {
            for (Player player : teams.get(i)) {
                double weight = weights.containsKey(player.id) ? weights.get(player.id) : 1;
                if (!finite(weight) || weight <= 0) {
                    throw new IllegalArgumentException("Player weights must be finite and positive.");
                }
                Rating rating = rating(prior, player.id);
                means[i] += rating.mean;
                variances[i] += rating.sigma * rating.sigma + TAU * TAU;
            }
        }
        double c = Math.sqrt(variances[0] + variances[1] + 2 * BETA * BETA);
        double difference = means[0] - means[1];
        double scoreGap = Math.abs((double) game.team1Score - game.team2Score);
        double factor = margin > 0 && scoreGap > margin ? Math.log1p(scoreGap / margin) : 1;
        // These models are equivalent for two teams without margins. With scores,
        // Python BT divides the difference; PL adjusts both team means instead.
        double adjustedDifference = model == Model.BRADLEY_TERRY
                ? difference / factor : difference * (3 - 2 * factor);
        double p = logistic(adjustedDifference / c);
        double outcome = game.team1Score == game.team2Score ? 0.5
                : (game.team1Score > game.team2Score ? 1 : 0);
        Map<Long, Rating> result = new HashMap<>(prior);
        for (int i = 0; i < 2; i++) {
            double omega = variances[i] / c * (i == 0 ? outcome - p : p - outcome);
            double delta = Math.sqrt(variances[i]) / c * variances[i] / (c * c) * p * (1 - p);
            for (Player player : teams.get(i)) {
                Rating old = rating(prior, player.id);
                double variance = old.sigma * old.sigma + TAU * TAU;
                double weight = weights.containsKey(player.id) ? weights.get(player.id) : 1;
                double multiplier = omega >= 0 ? weight : 1 / weight;
                result.put(player.id, new Rating(old.mean + variance / variances[i] * omega * multiplier,
                        Math.sqrt(variance * Math.max(1 - variance / variances[i] * delta * multiplier, 1e-4))));
            }
        }
        return result;
    }

    static void validateGame(Game game) {
        if (game.deleted || game.team1Score < 0 || game.team2Score < 0
                || game.team1.isEmpty() || game.team2.isEmpty()) {
            throw new IllegalArgumentException("Require an active game, nonnegative scores, and two nonempty teams.");
        }
        HashSet<Long> ids = new HashSet<>();
        for (List<Player> team : Arrays.asList(game.team1, game.team2)) {
            for (Player player : team) {
                if (!ids.add(player.id)) {
                    throw new IllegalArgumentException("A player can participate only once.");
                }
            }
        }
    }

    private static double logistic(double value) {
        // Avoid overflow for surprising outcomes after long histories.
        double exp = Math.exp(-Math.abs(value));
        return value >= 0 ? 1 / (1 + exp) : exp / (1 + exp);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
