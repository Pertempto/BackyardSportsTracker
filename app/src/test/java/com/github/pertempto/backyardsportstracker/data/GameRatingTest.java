package com.github.pertempto.backyardsportstracker.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class GameRatingTest {
    @Test
    public void zeroPointGameDoesNotCorruptRatingsDuringHistoryReplay() {
        Player team1Player = new Player("Alex", new HashMap<String, Double>());
        team1Player.id = 1;
        Player team2Player = new Player("Sam", new HashMap<String, Double>());
        team2Player.id = 2;

        HashMap<Long, Double> initialRatings = new HashMap<>();
        initialRatings.put(team1Player.id, 1000.0);
        initialRatings.put(team2Player.id, 1000.0);
        Game game = new Game(Sports.BASKETBALL, new Date(0), initialRatings, 0, 0,
                Arrays.asList(team1Player), Arrays.asList(team2Player));

        assertEquals(1.0, game.getTeam1ChangeFactor(), 0.0);
        assertEquals(1.0, game.getTeam2ChangeFactor(), 0.0);
    }
}
