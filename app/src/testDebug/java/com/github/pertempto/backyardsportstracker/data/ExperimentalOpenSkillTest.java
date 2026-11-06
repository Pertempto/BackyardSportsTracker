package com.github.pertempto.backyardsportstracker.data;

import com.google.gson.Gson;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ExperimentalOpenSkillTest {
    private static class Fixtures { Fixture[] fixtures; }
    private static class Fixture {
        String name;
        String model;
        double margin;
        long[][] teams;
        int[] scores;
        double[][] priors;
        double[][] weights;
        double[][][] expected;
    }

    @Test
    public void matchesPinnedPythonFixturesIncludingMarginsTiesUnevenTeamsAndWeights() throws Exception {
        Fixtures fixtures;
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/openskill-fixtures.json"), "UTF-8")) {
            fixtures = new Gson().fromJson(reader, Fixtures.class);
        }
        assertEquals(52, fixtures.fixtures.length);
        for (Fixture fixture : fixtures.fixtures) {
            Map<Long, ExperimentalOpenSkill.Rating> priors = new HashMap<>();
            Map<Long, Double> weights = new HashMap<>();
            List<List<Player>> teams = new ArrayList<>();
            int index = 0;
            for (int t = 0; t < 2; t++) {
                List<Player> team = new ArrayList<>();
                for (int p = 0; p < fixture.teams[t].length; p++) {
                    long id = fixture.teams[t][p];
                    team.add(player(id));
                    priors.put(id, new ExperimentalOpenSkill.Rating(fixture.priors[index][0], fixture.priors[index][1]));
                    weights.put(id, fixture.weights[t][p]);
                    index++;
                }
                teams.add(team);
            }
            ExperimentalOpenSkill.Model model = fixture.model.equals("BradleyTerryFull")
                    ? ExperimentalOpenSkill.Model.BRADLEY_TERRY : ExperimentalOpenSkill.Model.PLACKETT_LUCE;
            Game game = game(teams.get(0), teams.get(1), fixture.scores[0], fixture.scores[1]);
            Map<Long, ExperimentalOpenSkill.Rating> result = ExperimentalOpenSkill.rate(game, priors, model, fixture.margin, weights);
            Map<Long, ExperimentalOpenSkill.Rating> swapped = ExperimentalOpenSkill.rate(
                    game(teams.get(1), teams.get(0), fixture.scores[1], fixture.scores[0]), priors, model, fixture.margin, weights);
            for (int t = 0; t < 2; t++) {
                for (int p = 0; p < fixture.teams[t].length; p++) {
                    long id = fixture.teams[t][p];
                    String label = fixture.name + " " + fixture.model + " margin=" + fixture.margin + " player=" + id;
                    assertEquals(label, fixture.expected[t][p][0], result.get(id).mean, 1e-9);
                    assertEquals(label, fixture.expected[t][p][1], result.get(id).sigma, 1e-9);
                    assertEquals(label, result.get(id).mean, swapped.get(id).mean, 1e-9);
                    assertEquals(label, result.get(id).sigma, swapped.get(id).sigma, 1e-9);
                }
            }
            assertEquals(fixture.priors[0][0], priors.get(fixture.teams[0][0]).mean, 0);
            assertEquals(fixture.scores[0], game.team1Score);
        }
    }

    @Test
    public void carriesUncertaintyAcrossRotatingRostersWithoutChangingPriorOrNonparticipants() {
        Map<Long, ExperimentalOpenSkill.Rating> initial = new HashMap<>();
        initial.put(9L, new ExperimentalOpenSkill.Rating(34, 2));
        for (ExperimentalOpenSkill.Model model : ExperimentalOpenSkill.Model.values()) {
            Map<Long, ExperimentalOpenSkill.Rating> first = ExperimentalOpenSkill.rate(
                    game(team(1, 2), team(3, 4), 10, 3), initial, model, 2, Collections.<Long, Double>emptyMap());
            Map<Long, ExperimentalOpenSkill.Rating> second = ExperimentalOpenSkill.rate(
                    game(team(1, 3), team(2, 4), 10, 7), first, model, 2, Collections.<Long, Double>emptyMap());
            assertTrue(second.get(1L).mean > second.get(2L).mean);
            assertTrue(second.get(3L).mean > second.get(4L).mean);
            assertTrue(second.get(1L).sigma < first.get(1L).sigma);
            assertSame(initial.get(9L), second.get(9L));
            assertEquals(1, initial.size());
        }
    }

    @Test
    public void rejectsInvalidInputWithoutPartialUpdates() {
        Map<Long, ExperimentalOpenSkill.Rating> prior = new HashMap<>();
        for (Game bad : Arrays.asList(game(team(), team(2), 1, 0), game(team(1), team(1), 1, 0),
                game(team(1, 1), team(2), 1, 0), game(team(1), team(2), -1, 0))) {
            try {
                ExperimentalOpenSkill.rate(bad, prior, ExperimentalOpenSkill.Model.BRADLEY_TERRY, 2,
                        Collections.<Long, Double>emptyMap());
                fail("Invalid game accepted");
            } catch (IllegalArgumentException expected) { assertTrue(prior.isEmpty()); }
        }
        for (double weight : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                ExperimentalOpenSkill.rate(game(team(1), team(2), 1, 0), prior,
                        ExperimentalOpenSkill.Model.PLACKETT_LUCE, 2, Collections.singletonMap(1L, weight));
                fail("Invalid weight accepted");
            } catch (IllegalArgumentException expected) { assertTrue(prior.isEmpty()); }
        }
    }

    static Player player(long id) {
        Player player = new Player("Player " + id, new HashMap<String, Double>());
        player.id = id;
        return player;
    }

    static List<Player> team(long... ids) {
        List<Player> team = new ArrayList<>();
        for (long id : ids) team.add(player(id));
        return team;
    }

    static Game game(List<Player> first, List<Player> second, int score1, int score2) {
        return new Game(Sports.BASKETBALL, new Date(0), new HashMap<Long, Double>(), score1, score2, first, second);
    }
}
