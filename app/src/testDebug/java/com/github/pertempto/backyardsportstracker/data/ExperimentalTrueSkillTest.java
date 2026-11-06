package com.github.pertempto.backyardsportstracker.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ExperimentalTrueSkillTest {
    private final Player a = player(1);
    private final Player b = player(2);
    private final Player c = player(3);
    private final Player d = player(4);

    @Test
    public void matchesIndependentReferenceForWinDrawAndUnequalTeamUpset() {
        // trueskill 0.4.5, mpmath backend, default parameters; not derived from this implementation.
        Game win = game(1, 10, 4, team(a), team(b));
        ExperimentalTrueSkill model = replay(win);
        assertRating(model, a, 29.39583169299151, 7.17147580700922);
        assertRating(model, b, 20.60416830700848, 7.17147580700922);
        assertEquals(7.88140427196385, model.rating(a.id).conservative(), 1e-6);

        assertRating(replay(game(1, 0, 0, team(a), team(b))), a, 25, 6.457515683245051);
        model = replay(game(1, 4, 10, team(a, b), team(c)));
        assertRating(model, a, 16.26932885100357, 7.317365362867209);
        assertRating(model, c, 33.73067114899642, 7.317365362867209);
        // Swapping the sides must not change who gained skill.
        assertRating(replay(game(1, 10, 4, team(c), team(a, b))), c,
                model.rating(c.id).mean, model.rating(c.id).sigma);
    }

    @Test
    public void carriesIndividualUncertaintyAcrossChangingTeamsInReplayOrder() {
        Game first = game(1, 10, 4, team(a, b), team(c, d));
        Game second = game(2, 10, 7, team(a, c), team(b, d));
        List<Game> games = Arrays.asList(second, first);
        ExperimentalTrueSkill model = ExperimentalTrueSkill.fromGames(games, Sports.BASKETBALL);
        assertRating(model, a, 30.972955411050837, 7.268023290842246);
        assertRating(model, b, 25.243689387087237, 7.268023290842246);
        assertRating(model, c, 24.756310612912763, 7.268023290842246);
        assertRating(model, d, 19.027044588949153, 7.268023290842246);
        assertSame(second, games.get(0));
        assertEquals(1000, first.initialRatings.get(a.id), 0);
        assertEquals(1000, a.ratings.get(Sports.BASKETBALL), 0);
    }

    @Test
    public void separatesSportsAndRebuildsAfterEditsAndDeletions() {
        Game basketball = game(1, 10, 4, team(a), team(b));
        Game ultimate = game(2, 3, 1, team(b), team(a));
        ultimate.sport = Sports.ULTIMATE;
        List<Game> history = Arrays.asList(ultimate, basketball);
        ExperimentalTrueSkill model = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        assertRating(model, a, 29.39583169299151, 7.17147580700922);
        assertRating(ExperimentalTrueSkill.fromGames(history, Sports.ULTIMATE), a,
                20.60416830700848, 7.17147580700922);
        basketball.team1Score = 1;
        assertRating(ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL), a,
                20.60416830700848, 7.17147580700922);
        basketball.deleted = true;
        assertRating(ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL), a, 25, 25.0 / 3);
        // Previously computed results are immutable snapshots.
        assertRating(model, a, 29.39583169299151, 7.17147580700922);
    }

    @Test
    public void uncertainNewTeammateMovesMoreThanEstablishedTeammate() {
        List<Game> history = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            history.add(game(i, 5, 5, team(a), team(b)));
        }
        ExperimentalTrueSkill before = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        history.add(game(51, 10, 4, team(a, c), team(b, d)));
        ExperimentalTrueSkill after = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        assertTrue(after.rating(c.id).mean - 25 > after.rating(a.id).mean - before.rating(a.id).mean);
        assertTrue(after.rating(c.id).sigma < 25.0 / 3);
        assertTrue(after.rating(a.id).sigma > 0);
        Game upset = game(52, 0, 10, team(a, c), team(b, d));
        history.add(upset);
        ExperimentalTrueSkill reversed = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        assertTrue(reversed.rating(c.id).mean < after.rating(c.id).mean);
    }

    @Test
    public void qualityAccountsForUncertaintyAndPrefersMixedTeams() {
        ExperimentalTrueSkill prior = replay();
        assertEquals(Math.sqrt(0.2), prior.matchQuality(team(a), team(b)), 1e-12);
        ExperimentalTrueSkill draw = replay(game(1, 5, 5, team(a), team(b)));
        assertTrue(draw.matchQuality(team(a), team(b)) > prior.matchQuality(team(a), team(b)));
        ExperimentalTrueSkill model = replay(game(1, 10, 2, team(a, b), team(c, d)));
        double mixed = model.matchQuality(team(a, c), team(b, d));
        assertTrue(mixed > model.matchQuality(team(a, b), team(c, d)));
        assertEquals(mixed, model.matchQuality(team(b, d), team(a, c)), 1e-12);
    }

    @Test
    public void comparisonShowsOutcomeOnlyUpdatesUnlikeBothScoreBasedApproaches() {
        Game close = game(1, 10, 9, team(a), team(b));
        Game blowout = game(1, 10, 0, team(a), team(b));
        assertRating(replay(close), a, replay(blowout).rating(a.id).mean,
                replay(blowout).rating(a.id).sigma);
        assertEquals(1000 * (1 + 0.2 / 19), 1000 * close.getTeam1ChangeFactor(), 1e-9);
        assertEquals(1200, 1000 * blowout.getTeam1ChangeFactor(), 1e-9);
        TeamStrength closeFit = TeamStrength.fromGames(Arrays.asList(close), Sports.BASKETBALL);
        TeamStrength blowoutFit = TeamStrength.fromGames(Arrays.asList(blowout), Sports.BASKETBALL);
        assertTrue(blowoutFit.forTeam(team(a)) - blowoutFit.forTeam(team(b))
                > closeFit.forTeam(team(a)) - closeFit.forTeam(team(b)));
    }

    @Test
    public void longHistoryAndSurprisingDrawRemainFinite() {
        List<Game> history = new ArrayList<>();
        for (int i = 1; i <= 2000; i++) {
            history.add(game(i, 10, 0, team(a), team(b)));
        }
        ExperimentalTrueSkill before = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        history.add(game(2001, 5, 5, team(a), team(b)));
        ExperimentalTrueSkill after = ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL);
        assertTrue(after.rating(a.id).mean < before.rating(a.id).mean);
        assertTrue(after.rating(b.id).mean > before.rating(b.id).mean);
        assertTrue(after.rating(a.id).sigma > 0 && after.rating(a.id).sigma < 25.0 / 3);
        history.add(game(2002, 0, 10, team(a), team(b)));
        assertTrue(ExperimentalTrueSkill.fromGames(history, Sports.BASKETBALL).rating(a.id).mean
                < after.rating(a.id).mean);
    }

    @Test
    public void rejectsInvalidRostersScoresAndAllSports() {
        for (Game invalid : Arrays.asList(game(1, 1, 0, team(), team(b)),
                game(1, 1, 0, team(a, a), team(b)), game(1, 1, 0, team(a), team(a)),
                game(1, -1, 0, team(a), team(b)))) {
            try {
                replay(invalid);
                fail("Invalid game accepted");
            } catch (IllegalArgumentException expected) {
                // Invalid input must not silently produce a rating.
            }
        }
        try {
            ExperimentalTrueSkill.fromGames(Collections.<Game>emptyList(), Sports.ALL);
            fail("Cross-sport pooling accepted");
        } catch (IllegalArgumentException expected) {
            // Skills are sport-specific.
        }
    }

    private static ExperimentalTrueSkill replay(Game... games) {
        return ExperimentalTrueSkill.fromGames(Arrays.asList(games), Sports.BASKETBALL);
    }

    private static void assertRating(ExperimentalTrueSkill model, Player player, double mean, double sigma) {
        assertEquals(mean, model.rating(player.id).mean, 1e-6);
        assertEquals(sigma, model.rating(player.id).sigma, 1e-6);
    }

    private static Player player(long id) {
        HashMap<String, Double> ratings = new HashMap<>();
        ratings.put(Sports.BASKETBALL, 1000.0);
        Player player = new Player("Player " + id, ratings);
        player.id = id;
        return player;
    }

    private static List<Player> team(Player... players) {
        return Arrays.asList(players);
    }

    private static Game game(long id, int score1, int score2, List<Player> team1, List<Player> team2) {
        HashMap<Long, Double> initial = new HashMap<>();
        for (List<Player> team : Arrays.asList(team1, team2)) {
            for (Player player : team) {
                initial.put(player.id, 1000.0);
            }
        }
        Game game = new Game(Sports.BASKETBALL, new Date(0), initial, score1, score2, team1, team2);
        game.id = id;
        return game;
    }
}
