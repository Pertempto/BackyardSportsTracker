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
        TeamStrength model = TeamStrength.fromGames(games, Sports.ULTIMATE);
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
                        Math.abs(model.forTeam(grouping.team1) - model.forTeam(grouping.team2)));
            }
        }
        Grouping best = groupings.get(0);
        // Independent Python nonnegative fit: all 14 historical participants, not just this roster.
        assertEquals(ids(team(1, 2, 9, 10, 11)), ids(best.team1));
        assertEquals(ids(team(3, 5, 6, 7, 8, 12)), ids(best.team2));
        assertEquals(8.161109021443, model.forTeam(best.team1), 1e-6);
        assertEquals(8.163190290237, model.forTeam(best.team2), 1e-6);
        assertEquals(9.104560736445, model.forTeam(team(12, 8, 2, 1, 11)), 1e-6);
        assertEquals(7.302434256762, model.forTeam(team(3, 5, 9, 10, 6, 7)), 1e-6);
        assertEquals(6.285452536195, smallestThreeVsEightGap, 1e-6);
        for (Grouping grouping : groupings.subList(0, 10)) {
            assertEquals(5, Math.min(grouping.team1.size(), grouping.team2.size()));
        }
    }

    @Test
    public void fourPlayerRegressionPrefersTwoVsTwoAndKeepsAllSevenSplits() {
        List<Game> games = backupGames();
        ArrayList<Grouping> groupings = Util.generateGroupings(
                new ArrayList<>(team(2, 12, 10, 6)), Sports.ULTIMATE, games);
        assertEquals(7, groupings.size());
        assertCostsSorted(groupings, games);
        Grouping best = groupings.get(0);
        assertEquals(ids(team(2, 10)), ids(best.team1));
        assertEquals(ids(team(6, 12)), ids(best.team2));
        TeamStrength model = TeamStrength.fromGames(games, Sports.ULTIMATE);
        assertEquals(1.807536898476, model.forTeam(best.team1), 1e-6);
        assertEquals(1.953153458320, model.forTeam(best.team2), 1e-6);
        assertEquals(1.110613372455, model.forTeam(team(2)), 1e-6);
        assertEquals(2.650076984341, model.forTeam(team(12, 10, 6)), 1e-6);
        boolean hasSingleton = false;
        for (Grouping grouping : groupings) {
            hasSingleton |= Math.min(grouping.team1.size(), grouping.team2.size()) == 1;
        }
        assertTrue(hasSingleton);
    }

    @Test
    public void fullHistoryFitMatchesIndependentGameMarginsAndShutoutOrdering() {
        List<Game> games = backupGames();
        TeamStrength model = TeamStrength.fromGames(games, Sports.ULTIMATE);
        double[] expected = {-1.216143750717, -1.797087394709, -2.337715907927,
                2.332205775539, 1.606415940556, -2.749495669183, -1.646936717815,
                1.910799342606, 3.818300230254, -2.319429399284, 2.319429399284};
        double[] margins = new double[games.size()];
        for (int i = 0; i < games.size(); i++) {
            Game game = games.get(i);
            margins[i] = model.forTeam(game.team1) - model.forTeam(game.team2);
            assertEquals(expected[i], margins[i], 1e-6);
            assertEquals(Math.signum(game.team1Score - game.team2Score), Math.signum(margins[i]), 0);
        }
        double ordered = 0;
        for (int i = 0; i < games.size(); i++) {
            if (games.get(i).team1Score != 0 && games.get(i).team2Score != 0) {
                continue;
            }
            for (int j = 0; j < games.size(); j++) {
                if (games.get(j).team1Score == 0 || games.get(j).team2Score == 0) {
                    continue;
                }
                double difference = Math.abs(margins[i]) - Math.abs(margins[j]);
                ordered += Math.abs(difference) < 1e-9 ? 0.5 : difference > 0 ? 1 : 0;
            }
        }
        // In-sample explanation only: 7 shutouts x 4 nonshutouts, with one tied comparison.
        assertEquals(24.5, ordered, 1e-12);
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
        Grouping firstGrouping = groupings.get(0);
        assertEquals(model.forTeam(firstGrouping.team1),
                Util.teamStrength(firstGrouping.team1, games, Sports.ULTIMATE), 1e-12);
        assertEquals(model.forTeam(firstGrouping.team2),
                Util.teamStrength(firstGrouping.team2, games, Sports.ULTIMATE), 1e-12);
        for (Grouping grouping : groupings) {
            double first = model.forTeam(grouping.team1);
            double second = model.forTeam(grouping.team2);
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
