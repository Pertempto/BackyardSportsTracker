package com.github.pertempto.backyardsportstracker;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TeamBalancingTest {
    @Test
    public void ranksClosestPointDifferencesFirstEvenWhenTotalDifferenceIsZero() {
        verifyBalancing(new int[]{8, 3, -2, -9}, new int[]{2, 4, 6, 12, 16, 18, 22});
    }

    @Test
    public void handlesNegativeTeamStrengthsWithoutRatios() {
        verifyBalancing(new int[]{-8, -3, -2, -9}, new int[]{0, 2, 4, 6, 12, 16, 18});
    }

    @Test
    public void playersWithoutHistoryHaveZeroStrength() {
        ArrayList<Player> players = new ArrayList<>(Arrays.asList(player(1), player(2), player(3)));
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE,
                Collections.<Game>emptyList());
        assertEquals(3, groupings.size());
        for (Grouping grouping : groupings) {
            assertTrue(!grouping.team1.isEmpty() && !grouping.team2.isEmpty());
            assertEquals(3, grouping.team1.size() + grouping.team2.size());
        }
    }

    private static void verifyBalancing(int[] differences, int[] expectedGaps) {
        ArrayList<Player> players = new ArrayList<>();
        List<Game> games = new ArrayList<>();
        Player opponent = player(99);
        for (int i = 0; i < differences.length; i++) {
            Player player = player(i + 1);
            players.add(player);
            games.add(new Game(Sports.ULTIMATE, new Date(0), new HashMap<Long, Double>(),
                    Math.max(differences[i], 0), Math.max(-differences[i], 0),
                    Arrays.asList(player), Arrays.asList(opponent)));
        }
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        assertEquals(expectedGaps.length, groupings.size());
        for (int i = 0; i < groupings.size(); i++) {
            Grouping grouping = groupings.get(i);
            // Use the supplied differences, not the aggregation code under test.
            int first = sum(grouping.team1, differences);
            int second = sum(grouping.team2, differences);
            assertTrue(first <= second);
            assertEquals(expectedGaps[i], second - first);
            assertEquals(4, grouping.team1.size() + grouping.team2.size());
        }
    }

    private static int sum(List<Player> players, int[] differences) {
        int total = 0;
        for (Player player : players) {
            total += differences[(int) player.id - 1];
        }
        return total;
    }

    private static Player player(long id) {
        HashMap<String, Double> ratings = new HashMap<>();
        ratings.put(Sports.ULTIMATE, (double) id * id);
        Player player = new Player("Player " + id, ratings);
        player.id = id;
        return player;
    }
}
