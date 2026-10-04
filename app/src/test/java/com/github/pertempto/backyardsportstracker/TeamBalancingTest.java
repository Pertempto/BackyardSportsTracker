package com.github.pertempto.backyardsportstracker;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;
import com.github.pertempto.backyardsportstracker.data.TeamStrength;

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
    public void backupRosterMatchesIndependentExperimentAndRetainsEverySplit() {
        List<Game> games = backupGames();
        ArrayList<Player> players = new ArrayList<>(team(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12));
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE, games);
        assertEquals(1023, groupings.size());
        assertCostsSorted(groupings, games);
        Set<Set<Long>> seen = new HashSet<>();
        double smallestThreeVsEightGap = Double.POSITIVE_INFINITY;
        for (Grouping grouping : groupings) {
            assertTrue(!grouping.team1.isEmpty() && !grouping.team2.isEmpty());
            Set<Long> all = ids(grouping.team1);
            for (Player player : grouping.team2) {
                assertTrue(all.add(player.id));
            }
            assertEquals(ids(players), all);
            List<Player> anchored = grouping.team1.contains(player(1)) ? grouping.team1 : grouping.team2;
            assertTrue(seen.add(ids(anchored)));
            if (Math.min(grouping.team1.size(), grouping.team2.size()) == 3) {
                smallestThreeVsEightGap = Math.min(smallestThreeVsEightGap,
                        Math.abs(Util.teamStrength(grouping.team1, games, Sports.ULTIMATE)
                                - Util.teamStrength(grouping.team2, games, Sports.ULTIMATE)));
            }
        }
        Grouping best = groupings.get(0);
        // Expected rosters and strengths come from the independent Python experiment.
        assertEquals(ids(team(1, 12, 6, 9, 5)), ids(best.team1));
        assertEquals(ids(team(2, 3, 8, 7, 10, 11)), ids(best.team2));
        assertEquals(3.067668, Util.teamStrength(best.team1, games, Sports.ULTIMATE), 1e-6);
        assertEquals(3.068813, Util.teamStrength(best.team2, games, Sports.ULTIMATE), 1e-6);
        assertEquals(3.5, Util.teamStrength(team(12, 8, 2, 1, 11), games, Sports.ULTIMATE), 1e-12);
        assertEquals(1.8, Util.teamStrength(team(3, 5, 9, 10, 6, 7), games, Sports.ULTIMATE), 1e-12);
        assertEquals(0.3, smallestThreeVsEightGap, 1e-12);
        for (Grouping grouping : groupings.subList(0, 10)) {
            assertTrue(Math.min(grouping.team1.size(), grouping.team2.size()) > 3);
        }
    }

    @Test
    public void zeroScoreHistoryStillIncludesUnequalSplits() {
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

    private static void assertCostsSorted(List<Grouping> groupings, List<Game> games) {
        double previous = -1;
        TeamStrength model = TeamStrength.fromGames(games, Sports.ULTIMATE);
        for (Grouping grouping : groupings) {
            double first = Util.teamStrength(grouping.team1, games, Sports.ULTIMATE);
            double second = Util.teamStrength(grouping.team2, games, Sports.ULTIMATE);
            assertEquals(model.forTeam(grouping.team1), first, 1e-12);
            assertEquals(model.forTeam(grouping.team2), second, 1e-12);
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

    private static List<Game> backupGames() {
        return Arrays.asList(
                game(Sports.ULTIMATE, 4, 6, team(2, 7, 6, 10), team(1, 11, 5)),
                game(Sports.ULTIMATE, 2, 4, team(1, 2, 6, 7), team(3, 8, 10, 12)),
                game(Sports.ULTIMATE, 0, 3, team(11, 2, 7, 6, 10), team(1, 9, 12, 5)),
                game(Sports.ULTIMATE, 3, 0, team(2, 6, 7, 9, 11), team(1, 5, 10, 12)),
                game(Sports.ULTIMATE, 3, 1, team(1, 2, 4, 10), team(6, 7, 11, 12)),
                game(Sports.ULTIMATE, 0, 3, team(6, 8, 9, 10, 14, 4), team(1, 2, 5, 7, 13, 3)),
                game(Sports.ULTIMATE, 0, 3, team(1, 7, 10), team(2, 5, 6)),
                game(Sports.ULTIMATE, 3, 0, team(3, 8, 9, 11, 12), team(1, 2, 5, 6, 7, 10)),
                game(Sports.ULTIMATE, 3, 0, team(3, 8, 9, 11, 12), team(1, 2, 6, 7, 10)),
                game(Sports.ULTIMATE, 1, 3, team(3, 9, 11, 7, 6), team(12, 8, 2, 1, 10)),
                game(Sports.ULTIMATE, 3, 0, team(12, 8, 2, 1, 10), team(3, 9, 11, 7, 6)));
    }

    private static List<Player> team(long... ids) {
        List<Player> players = new ArrayList<>();
        for (long id : ids) {
            players.add(player(id));
        }
        return players;
    }

    private static Set<Long> ids(List<Player> players) {
        Set<Long> ids = new HashSet<>();
        for (Player player : players) {
            ids.add(player.id);
        }
        return ids;
    }

    private static Player player(long id) {
        HashMap<String, Double> ratings = new HashMap<>();
        ratings.put(Sports.ULTIMATE, (double) id * id);
        Player player = new Player("Player " + id, ratings);
        player.id = id;
        return player;
    }
}
