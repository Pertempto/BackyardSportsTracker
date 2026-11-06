package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Read-only, two-team TrueSkill experiment. Not included in release builds.
 * See docs/trueskill-experiment.md before using or distributing this experiment.
 */
public final class ExperimentalTrueSkill {
    private static final double BETA = 25.0 / 6;
    private static final double TAU = 25.0 / 300;
    // Inverse normal CDF at 0.55: a provisional 10% draw probability.
    private static final double DRAW_QUANTILE = 0.125661346855074;
    private final Map<Long, Rating> ratings = new HashMap<>();

    public static final class Rating {
        public final double mean;
        public final double sigma;

        private Rating(double mean, double sigma) {
            this.mean = mean;
            this.sigma = sigma;
        }

        public double conservative() {
            return mean - 3 * sigma;
        }
    }

    private ExperimentalTrueSkill() {}

    /** Rebuild after edits/deletions; never mutate games, snapshots, or persisted ratings. */
    public static ExperimentalTrueSkill fromGames(List<Game> games, String sport) {
        if (sport == null || !Sports.sports.contains(sport)) {
            throw new IllegalArgumentException("Choose one supported sport, not all sports.");
        }
        List<Game> history = new ArrayList<>();
        for (Game game : games) {
            if (!game.deleted && sport.equals(game.sport)) {
                history.add(game);
            }
        }
        // Match the app's existing replay order, not the editable display date.
        Collections.sort(history, new Comparator<Game>() {
            @Override
            public int compare(Game first, Game second) {
                return Long.compare(first.id, second.id);
            }
        });
        ExperimentalTrueSkill model = new ExperimentalTrueSkill();
        for (Game game : history) {
            validateTeams(game.team1, game.team2);
            if (game.team1Score < 0 || game.team2Score < 0) {
                throw new IllegalArgumentException("Scores must be nonnegative.");
            }
            model.update(game);
        }
        return model;
    }

    public Rating rating(long playerId) {
        Rating rating = ratings.get(playerId);
        return rating == null ? new Rating(25, 25.0 / 3) : rating;
    }

    /** Hypothetical draw-based match quality, not a win probability. Higher is better. */
    public double matchQuality(List<Player> team1, List<Player> team2) {
        validateTeams(team1, team2);
        double noise = (team1.size() + team2.size()) * BETA * BETA;
        double variance = noise;
        double difference = 0;
        for (Player player : team1) {
            Rating rating = rating(player.id);
            difference += rating.mean;
            variance += rating.sigma * rating.sigma;
        }
        for (Player player : team2) {
            Rating rating = rating(player.id);
            difference -= rating.mean;
            variance += rating.sigma * rating.sigma;
        }
        return Math.sqrt(noise / variance) * Math.exp(-difference * difference / (2 * variance));
    }

    private void update(Game game) {
        List<Player> winners = game.team1Score >= game.team2Score ? game.team1 : game.team2;
        List<Player> losers = game.team1Score >= game.team2Score ? game.team2 : game.team1;
        List<Player> participants = new ArrayList<>(winners);
        participants.addAll(losers);
        double difference = 0;
        double variance = participants.size() * BETA * BETA;
        for (Player player : participants) {
            Rating rating = rating(player.id);
            difference += (winners.contains(player) ? 1 : -1) * rating.mean;
            variance += rating.sigma * rating.sigma + TAU * TAU;
        }
        double c = Math.sqrt(variance);
        double margin = DRAW_QUANTILE * BETA * Math.sqrt(participants.size());
        boolean draw = game.team1Score == game.team2Score;
        double[] moments = truncatedMoments((margin - difference) / c,
                draw ? (-margin - difference) / c : Double.POSITIVE_INFINITY, draw);
        for (Player player : participants) {
            Rating rating = rating(player.id);
            double priorVariance = rating.sigma * rating.sigma + TAU * TAU;
            double sign = winners.contains(player) ? 1 : -1;
            ratings.put(player.id, new Rating(rating.mean + sign * priorVariance / c * moments[0],
                    Math.sqrt(priorVariance * (1 - priorVariance / variance * moments[1]))));
        }
    }

    /** Conditional standard-normal mean and 1-variance, evaluated without CDF tail subtraction. */
    private static double[] truncatedMoments(double threshold, double other, boolean draw) {
        double lower = draw ? other : threshold;
        double upper = draw ? threshold : Double.POSITIVE_INFINITY;
        double mode = Math.max(lower, Math.min(0, upper));
        // Omitted tails are at least exp(-72) below the density at the mode.
        lower = Math.max(lower, mode - 12);
        upper = Math.min(upper, mode + 12);
        int steps = 1024;
        double step = (upper - lower) / steps;
        double mass = 0;
        double first = 0;
        double second = 0;
        for (int i = 0; i <= steps; i++) {
            double offset = lower - mode + i * step;
            double density = Math.exp(-offset * (offset + 2 * mode) / 2);
            double weight = (i == 0 || i == steps) ? 1 : (i % 2 == 0 ? 2 : 4);
            mass += weight * density;
            first += weight * density * offset;
            second += weight * density * offset * offset;
        }
        double meanOffset = first / mass;
        double conditionalVariance = second / mass - meanOffset * meanOffset;
        return new double[]{mode + meanOffset, Math.max(0, Math.min(1, 1 - conditionalVariance))};
    }

    private static void validateTeams(List<Player> team1, List<Player> team2) {
        if (team1.isEmpty() || team2.isEmpty()) {
            throw new IllegalArgumentException("Both teams must have players.");
        }
        HashSet<Long> ids = new HashSet<>();
        for (List<Player> team : java.util.Arrays.asList(team1, team2)) {
            for (Player player : team) {
                if (!ids.add(player.id)) {
                    throw new IllegalArgumentException("A player can participate only once per game.");
                }
            }
        }
    }
}
