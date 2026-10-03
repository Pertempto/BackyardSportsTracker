package com.github.pertempto.backyardsportstracker.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GameHistoryReplayerTest {
    @Test
    public void editingEarlierGameRecalculatesLaterSnapshotsAndPlayerRatings() {
        Player team1Player = new Player("Alex", new HashMap<String, Double>());
        team1Player.id = 1;
        Player team2Player = new Player("Sam", new HashMap<String, Double>());
        team2Player.id = 2;

        Game earlier = game(1, team1Player, team2Player, 10, 0);
        Game later = game(2, team1Player, team2Player, 10, 0);
        List<Game> games = Arrays.asList(later, earlier);

        Map<Long, Double> finalRatings = GameHistoryReplayer.replay(games, earlier.id, 0, 10);

        assertEquals(0, earlier.team1Score);
        assertEquals(10, earlier.team2Score);
        double expectedLaterTeam1Start = 1000.0 * earlier.getTeam1ChangeFactor();
        assertEquals(expectedLaterTeam1Start, later.initialRatings.get(team1Player.id), 0.000001);
        assertEquals(expectedLaterTeam1Start * later.getTeam1ChangeFactor(),
                finalRatings.get(team1Player.id), 0.000001);
    }

    @Test
    public void addingAPlayerToOldGameUsesThatPlayersHistoricalRating() {
        Player team1Player = new Player("Alex", new HashMap<String, Double>());
        team1Player.id = 1;
        Player team2Player = new Player("Sam", new HashMap<String, Double>());
        team2Player.id = 2;
        Player addedPlayer = new Player("Taylor", new HashMap<String, Double>());
        addedPlayer.id = 3;

        Game earlier = game(1, team1Player, team2Player, 10, 0);
        Game later = game(2, team1Player, addedPlayer, 10, 0);
        later.initialRatings.put(addedPlayer.id, 700.0);
        earlier.team1 = Arrays.asList(team1Player, addedPlayer);
        HashMap<Long, Double> currentRatings = new HashMap<>();
        currentRatings.put(addedPlayer.id, 650.0);

        Map<Long, Double> finalRatings = GameHistoryReplayer.replay(
                Arrays.asList(earlier, later), earlier.id, 0, 10, currentRatings);

        double addedPlayerAfterEarlier = 700.0 * earlier.getTeam1ChangeFactor();
        assertEquals(addedPlayerAfterEarlier, later.initialRatings.get(addedPlayer.id), 0.000001);
        assertEquals(addedPlayerAfterEarlier * later.getTeam2ChangeFactor(),
                finalRatings.get(addedPlayer.id), 0.000001);
    }

    private Game game(long id, Player team1Player, Player team2Player, int team1Score, int team2Score) {
        HashMap<Long, Double> initialRatings = new HashMap<>();
        initialRatings.put(team1Player.id, 1000.0);
        initialRatings.put(team2Player.id, 1000.0);
        Game game = new Game(Sports.BASKETBALL, new Date(id), initialRatings, team1Score, team2Score,
                Arrays.asList(team1Player), Arrays.asList(team2Player));
        game.id = id;
        return game;
    }
}
