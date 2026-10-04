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
    public void familiarityChangesTheDisplayedStrengthUsedForRanking() {
        Player first = player(1);
        Player second = player(2);
        Player third = player(3);
        Player fourth = player(4);
        Player opponent = player(99);
        ArrayList<Player> players = new ArrayList<>(Arrays.asList(first, second, third, fourth));
        List<Game> games = new ArrayList<>();
        int[][] scores = {{103, 97}, {1, 1}, {1, 1}, {97, 103}};
        for (int i = 0; i < players.size(); i++) {
            games.add(game(Sports.ULTIMATE, scores[i][0], scores[i][1],
                    Arrays.asList(players.get(i)), Arrays.asList(opponent)));
        }
        // The individually balanced split has one familiar pair, giving a gap of 0.01.
        // Fresh 2v2 splits have a strength gap of 0.03, so balance wins initially.
        games.add(game(Sports.ULTIMATE, 0, 0,
                Arrays.asList(first, fourth), Arrays.asList(opponent)));
        for (int i = 0; i < 3; i++) {
            games.add(game(Sports.BASKETBALL, 0, 0,
                    Arrays.asList(first, fourth), Arrays.asList(opponent)));
            Game deleted = game(Sports.ULTIMATE, 0, 0,
                    Arrays.asList(first, fourth), Arrays.asList(opponent));
            deleted.deleted = true;
            games.add(deleted);
        }
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        assertEquals(7, groupings.size());
        assertTrue(together(groupings.get(0), first, fourth));
        assertEquals(1.01, Util.teamStrength(Arrays.asList(first, fourth), games, Sports.ULTIMATE), 1e-12);
        assertEquals(1.0, Util.teamStrength(Arrays.asList(second, third), games, Sports.ULTIMATE), 1e-12);
        assertCostsSorted(groupings, games);

        // Four shared games give a gap of 0.04: now a fresh split with gap 0.03 wins.
        // Reversed order, opposite side, and separately loaded objects must still count.
        for (int i = 0; i < 3; i++) {
            games.add(game(Sports.ULTIMATE, 0, 0,
                    Arrays.asList(opponent), Arrays.asList(player(4), player(1))));
        }
        groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        Grouping best = groupings.get(0);
        assertTrue(!together(best, first, fourth));
        assertEquals(1.04, Util.teamStrength(Arrays.asList(first, fourth), games, Sports.ULTIMATE), 1e-12);
        assertEquals(0.03, Math.abs(Util.teamStrength(best.team1, games, Sports.ULTIMATE)
                - Util.teamStrength(best.team2, games, Sports.ULTIMATE)), 1e-12);
        assertCostsSorted(groupings, games);
    }

    @Test
    public void countsEveryUnorderedPairInLargerTeamsAndStillIncludesUnequalSplits() {
        ArrayList<Player> players = new ArrayList<>(Arrays.asList(
                player(1), player(2), player(3), player(4), player(5)));
        List<Game> games = Arrays.asList(game(Sports.ULTIMATE, 0, 0,
                Arrays.asList(player(3), player(1), player(2), player(99)),
                Arrays.asList(player(5), player(4))));
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        assertEquals(15, groupings.size());
        assertCostsSorted(groupings, games);
        boolean includesOneVsFour = false;
        for (Grouping grouping : groupings) {
            includesOneVsFour |= Math.min(grouping.team1.size(), grouping.team2.size()) == 1;
        }
        assertTrue(includesOneVsFour);
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

    private static boolean together(Grouping grouping, Player first, Player second) {
        return (grouping.team1.contains(first) && grouping.team1.contains(second))
                || (grouping.team2.contains(first) && grouping.team2.contains(second));
    }

    private static void assertCostsSorted(List<Grouping> groupings, List<Game> games) {
        double previous = -1;
        for (Grouping grouping : groupings) {
            List<Double> strengths = new ArrayList<>();
            for (List<Player> current : Arrays.asList(grouping.team1, grouping.team2)) {
                long familiar = 0;
                for (Game game : games) {
                    if (game.deleted || !Sports.ULTIMATE.equals(game.sport)) {
                        continue;
                    }
                    for (List<Player> historical : Arrays.asList(game.team1, game.team2)) {
                        int shared = 0;
                        for (Player player : current) {
                            if (historical.contains(player)) {
                                shared++;
                            }
                        }
                        familiar += shared * (shared - 1) / 2;
                    }
                }
                double strength = PlayerStats.teamStrength(current, games, Sports.ULTIMATE)
                        + familiar / 100.0;
                assertEquals(strength, Util.teamStrength(current, games, Sports.ULTIMATE), 1e-12);
                strengths.add(strength);
            }
            double first = strengths.get(0);
            double second = strengths.get(1);
            assertTrue(first <= second);
            double cost = Math.abs(first - second);
            assertTrue(cost + 1e-12 >= previous);
            previous = cost;
        }
    }

    private static Game game(String sport, int scored, int conceded,
                             List<Player> first, List<Player> second) {
        return new Game(sport, new Date(0), new HashMap<Long, Double>(),
                scored, conceded, first, second);
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
