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
        // Independent constrained-QP fit: all 14 historical participants, not just this roster.
        assertEquals(ids(team(1, 3, 6, 8, 10, 12)), ids(best.team1));
        assertEquals(ids(team(2, 5, 7, 9, 11)), ids(best.team2));
        assertEquals(8.520147116423, model.forTeam(best.team1), 1e-6);
        assertEquals(8.522810247574, model.forTeam(best.team2), 1e-6);
        assertEquals(8.661356845202, model.forTeam(team(12, 8, 2, 1, 11)), 1e-6);
        assertEquals(6.876811125111, model.forTeam(team(3, 5, 9, 10, 6, 7)), 1e-6);
        assertEquals(5.223102181624, smallestThreeVsEightGap, 1e-6);
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
        assertEquals(1.875804405558, model.forTeam(best.team1), 1e-6);
        assertEquals(1.981989071342, model.forTeam(best.team2), 1e-6);
        assertEquals(1.094229361998, model.forTeam(team(2)), 1e-6);
        assertEquals(2.726241346551, model.forTeam(team(12, 10, 6)), 1e-6);
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
        double[] expected = {-1.239314980385, -1.846269260630, -2.385712712256,
                2.359310408427, 1.681340724919, -2.802611971142, -1.764087337643,
                2.039908493655, 3.708437969894, -2.334999677354, 2.334999677354};
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
    public void everyPlayerAndPairMatchesIndependentConstrainedFit() {
        TeamStrength model = TeamStrength.fromGames(backupGames(), Sports.ULTIMATE);
        double[] players = {0.993903372684, 1.094229361998, 1.027277165939, 1.013098955604,
                1.120749190065, 0.942956737122, 0.782594610748, 1.053799689225,
                1.097924174162, 0.781575043560, 1.039008093952, 1.039032334220,
                1.019738802886, 0.981626434608};
        // Upper triangle, ordered by historical player IDs 1..14. These are external
        // OSQP expectations, including zero effects, not values derived by the Java fit.
        double[] pairs = {
                0.4406636734, 0.1973880289, 0.3186592751, -0.0284207617, -0.0466449764,
                -0.1383136034, 0.3300006453, 0.6142872877, -0.0516440651, 0.7606850196,
                0.3035983415, 0.1973880289, 0,
                0.1973880289, 0.3186592751, 0.4732091849, 0.0962456707, 0, 0.3300006453,
                0.6406895916, 0, 0.0264023038, 0.3300006453, 0.1973880289, 0,
                0, 0.1973880289, 0, 0, 0.4053842758, -0.0783471088, 0.1537307394,
                -0.0783471088, 0.4053842758, 0.1973880289, 0,
                0, 0, 0, -0.1779514092, -0.1779514092, 0.1212712462, 0, 0, 0, -0.1506466593,
                0.2758211560, 0, 0, 0.6142872877, -0.1728292930, 0.7606850196,
                -0.0264023038, 0.1973880289, 0,
                -0.1239923067, 0, 0.1133009174, -0.0373227684, -0.2635183171, 0, 0, 0,
                0, 0.3106889463, -0.1289913953, 0, 0, 0.1973880289, 0,
                0.0542655076, 0.2863433558, 0.2516535365, 0.7353849211, 0, -0.1700832790,
                0, 0.5623424827, 0.8659408242, 0, -0.1700832790,
                0, 0, 0, 0,
                -0.0670057386, 0, 0,
                0, 0,
                0};
        assertEquals(91, pairs.length);
        int pair = 0;
        for (int i = 0; i < players.length; i++) {
            assertEquals(players[i], model.forTeam(team(i + 1)), 1e-6);
            for (int j = i + 1; j < players.length; j++) {
                double effect = model.forTeam(team(i + 1, j + 1))
                        - model.forTeam(team(i + 1)) - model.forTeam(team(j + 1));
                assertEquals(pairs[pair++], effect, 1e-6);
            }
        }
    }

    @Test
    public void negativePairsShareOneBudgetAcrossAllHistoricalPartners() {
        TeamStrength model = TeamStrength.fromGames(backupGames(), Sports.ULTIMATE);
        List<Player> players = team(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
        for (Player player : players) {
            double individual = model.forTeam(Arrays.asList(player));
            double penalties = 0;
            List<Player> worstPartners = new ArrayList<>();
            for (Player partner : players) {
                if (player.equals(partner)) {
                    continue;
                }
                double effect = model.forTeam(Arrays.asList(player, partner))
                        - individual - model.forTeam(Arrays.asList(partner));
                if (effect < -1e-8) {
                    penalties -= effect;
                    worstPartners.add(partner);
                }
            }
            assertTrue(penalties <= 0.5 * individual + 1e-8);
            // All negative partners form the worst subset for adding this player.
            // Checking it covers every possible subset, including unobserved teams.
            double before = model.forTeam(worstPartners);
            worstPartners.add(player);
            assertTrue(model.forTeam(worstPartners) - before >= 0.5 * individual - 1e-8);
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
