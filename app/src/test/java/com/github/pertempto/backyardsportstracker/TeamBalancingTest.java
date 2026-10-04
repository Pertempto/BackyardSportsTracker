package com.github.pertempto.backyardsportstracker;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.Sports;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TeamBalancingTest {
    @Test
    public void screenshotRosterIncludesAllSplitsAndRanksBySumOfIndividualShares() {
        int[][] points = {{14, 8}, {17, 28}, {20, 9}, {16, 6}, {13, 9},
                {24, 21}, {23, 22}, {17, 16}, {14, 31}, {14, 31}};
        ArrayList<Player> players = new ArrayList<>();
        List<Game> games = new ArrayList<>();
        Player opponent = player(99);
        for (int i = 0; i < points.length; i++) {
            Player player = player(i + 1);
            players.add(player);
            games.add(new Game(Sports.ULTIMATE, new Date(0), new HashMap<Long, Double>(),
                    points[i][0], points[i][1], Arrays.asList(player), Arrays.asList(opponent)));
        }
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        assertEquals(511, groupings.size());
        double previousGap = -1;
        for (Grouping grouping : groupings) {
            assertTrue(!grouping.team1.isEmpty() && !grouping.team2.isEmpty());
            assertEquals(10, grouping.team1.size() + grouping.team2.size());
            double first = strength(grouping.team1, points);
            double second = strength(grouping.team2, points);
            assertEquals(first, PlayerStats.teamStrength(grouping.team1, games, Sports.ULTIMATE), 1e-12);
            assertEquals(second, PlayerStats.teamStrength(grouping.team2, games, Sports.ULTIMATE), 1e-12);
            assertTrue(first <= second);
            double gap = second - first;
            assertTrue(gap + 1e-12 >= previousGap);
            previousGap = gap;
        }
        Grouping best = groupings.get(0);
        assertEquals(0.007836990595611049,
                strength(best.team2, points) - strength(best.team1, points), 1e-12);
        assertEquals(5, best.team1.size());
        assertEquals(5, best.team2.size());
    }

    @Test
    public void allNonemptySplitsAppearExactlyOnceEvenWithoutHistory() {
        ArrayList<Player> players = new ArrayList<>(Arrays.asList(
                player(1), player(2), player(3), player(4), player(5)));
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE,
                Collections.<Game>emptyList());
        assertEquals(15, groupings.size());
        Set<Set<Long>> seen = new HashSet<>();
        for (Grouping grouping : groupings) {
            assertTrue(!grouping.team1.isEmpty() && !grouping.team2.isEmpty());
            assertEquals(5, grouping.team1.size() + grouping.team2.size());
            Set<Long> all = new HashSet<>();
            for (Player player : grouping.team1) {
                assertTrue(all.add(player.id));
            }
            for (Player player : grouping.team2) {
                assertTrue(all.add(player.id));
            }
            assertEquals(new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L)), all);
            // Anchor player 1 to avoid counting a team swap as a new split.
            List<Player> anchored = grouping.team1.contains(player(1)) ? grouping.team1 : grouping.team2;
            Set<Long> ids = new HashSet<>();
            for (Player player : anchored) {
                ids.add(player.id);
            }
            assertTrue(seen.add(ids));
        }
        assertEquals(15, seen.size());
    }

    private static double strength(List<Player> players, int[][] points) {
        double strength = 0;
        for (Player player : players) {
            int[] record = points[(int) player.id - 1];
            strength += (double) record[0] / (record[0] + record[1]);
        }
        return strength;
    }

    private static Player player(long id) {
        HashMap<String, Double> ratings = new HashMap<>();
        ratings.put(Sports.ULTIMATE, (double) id * id);
        Player player = new Player("Player " + id, ratings);
        player.id = id;
        return player;
    }
}
