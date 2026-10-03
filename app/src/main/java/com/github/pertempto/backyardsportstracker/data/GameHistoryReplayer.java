package com.github.pertempto.backyardsportstracker.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameHistoryReplayer {
    private GameHistoryReplayer() {}

    public static Map<Long, Double> replay(List<Game> games, long editedGameId,
                                           int team1Score, int team2Score) {
        return replay(games, editedGameId, team1Score, team2Score, new HashMap<Long, Double>());
    }

    public static Map<Long, Double> replay(List<Game> games, long editedGameId,
                                           int team1Score, int team2Score,
                                           Map<Long, Double> currentPlayerRatings) {
        Collections.sort(games, new Comparator<Game>() {
            @Override
            public int compare(Game first, Game second) {
                return Long.compare(first.id, second.id);
            }
        });

        Map<Long, Double> initialPlayerRatings = new HashMap<>();
        for (Game game : games) {
            for (Map.Entry<Long, Double> initialRating : game.initialRatings.entrySet()) {
                if (!initialPlayerRatings.containsKey(initialRating.getKey())) {
                    initialPlayerRatings.put(initialRating.getKey(), initialRating.getValue());
                }
            }
            for (Player player : game.team1) {
                addInitialRating(game, player, initialPlayerRatings);
            }
            for (Player player : game.team2) {
                addInitialRating(game, player, initialPlayerRatings);
            }
        }
        for (Map.Entry<Long, Double> currentRating : currentPlayerRatings.entrySet()) {
            if (!initialPlayerRatings.containsKey(currentRating.getKey())) {
                initialPlayerRatings.put(currentRating.getKey(), currentRating.getValue());
            }
        }

        Map<Long, Double> currentRatings = new HashMap<>(initialPlayerRatings);
        boolean foundEditedGame = false;
        for (Game game : games) {
            if (game.id == editedGameId) {
                game.team1Score = team1Score;
                game.team2Score = team2Score;
                foundEditedGame = true;
            }

            HashMap<Long, Double> initialRatings = new HashMap<>();
            for (Player player : game.team1) {
                addGameRating(game, player, currentRatings, initialRatings);
            }
            for (Player player : game.team2) {
                addGameRating(game, player, currentRatings, initialRatings);
            }
            game.initialRatings = initialRatings;

            double team1ChangeFactor = game.getTeam1ChangeFactor();
            double team2ChangeFactor = game.getTeam2ChangeFactor();
            for (Player player : game.team1) {
                currentRatings.put(player.id, initialRatings.get(player.id) * team1ChangeFactor);
            }
            for (Player player : game.team2) {
                currentRatings.put(player.id, initialRatings.get(player.id) * team2ChangeFactor);
            }
        }

        if (!foundEditedGame) {
            throw new IllegalArgumentException("Game is no longer active.");
        }
        return currentRatings;
    }

    private static void addInitialRating(Game game, Player player, Map<Long, Double> initialRatings) {
        if (!initialRatings.containsKey(player.id)) {
            Double rating = game.initialRatings.get(player.id);
            if (rating != null) {
                initialRatings.put(player.id, rating);
            }
        }
    }

    private static void addGameRating(Game game, Player player, Map<Long, Double> currentRatings,
                                      Map<Long, Double> initialRatings) {
        Double rating = currentRatings.get(player.id);
        if (rating == null) {
            throw new IllegalStateException("Game is missing a player's initial rating.");
        }
        initialRatings.put(player.id, rating);
    }
}
