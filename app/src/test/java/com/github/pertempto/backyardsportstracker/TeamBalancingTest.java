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
    public void screenshotRosterProducesOnlyFiveVersusFiveAndRanksByScoringShare() {
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
        assertEquals(126, groupings.size());
        // Independently calculated strengths from the screenshot's totals, with a 3-3 prior.
        double[] strengths = {17.0 / 28, 20.0 / 51, 23.0 / 35, 19.0 / 28, 16.0 / 28,
                27.0 / 51, 26.0 / 51, 20.0 / 39, 17.0 / 51, 17.0 / 51};
        double previousGap = -1;
        for (Grouping grouping : groupings) {
            assertEquals(5, grouping.team1.size());
            assertEquals(5, grouping.team2.size());
            double first = sum(grouping.team1, strengths);
            double second = sum(grouping.team2, strengths);
            assertTrue(first <= second);
            double gap = second - first;
            assertTrue(gap + 1e-12 >= previousGap);
            previousGap = gap;
        }
        Grouping best = groupings.get(0);
        assertEquals(0.0027364792070674326,
                sum(best.team2, strengths) - sum(best.team1, strengths), 1e-12);
    }

    @Test
    public void oddRosterSizesDifferByOnlyOneEvenWithoutHistory() {
        ArrayList<Player> players = new ArrayList<>(Arrays.asList(
                player(1), player(2), player(3), player(4), player(5)));
        ArrayList<Grouping> groupings = Util.generateGroupings(players, Sports.ULTIMATE,
                Collections.<Game>emptyList());
        assertEquals(10, groupings.size());
        for (Grouping grouping : groupings) {
            assertEquals(1, Math.abs(grouping.team1.size() - grouping.team2.size()));
            assertEquals(5, grouping.team1.size() + grouping.team2.size());
        }
    }

    private static double sum(List<Player> players, double[] strengths) {
        double total = 0;
        for (Player player : players) {
            total += strengths[(int) player.id - 1];
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
