package com.github.pertempto.backyardsportstracker.data;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.pertempto.backyardsportstracker.data.ExperimentalOpenSkillTest.*;
import static org.junit.Assert.*;

public class ExperimentalRatingComparisonTest {
    @Test
    public void dateCutoffAndFrozenPredictionsPreventOutcomeSnapshotAndCurrentRatingLeakage() {
        Game early = dated(30, 1, team(1), team(2), 10, 0);
        Game later = dated(10, 2, team(2), team(1), 10, 9);
        Game heldOut = dated(20, 3, team(1), team(2), 10, 5);
        Game second = dated(40, 3, team(1), team(2), 0, 0);
        List<Game> history = Arrays.asList(heldOut, later, second, early);
        ExperimentalRatingComparison.Report before = evaluate(history, 3);
        assertEquals(2, before.trainingGames);
        assertEquals(2, before.heldOutGames);
        assertEquals(1, before.heldOutDraws);
        assertEquals(20, before.predictions.get(0).gameId);
        assertEquals(before.predictions.get(0).gaps, before.predictions.get(1).gaps);
        // Scores at the cutoff, final player ratings, and stored snapshots are not training inputs.
        heldOut.team1Score = 0;
        heldOut.team2Score = 100;
        for (Game game : history) {
            game.initialRatings.put(1L, 9999999.0);
            for (Player player : game.team1) player.ratings.put(Sports.BASKETBALL, 9999999.0);
        }
        ExperimentalRatingComparison.Report after = evaluate(history, 3);
        assertEquals(before.predictions.get(0).gaps, after.predictions.get(0).gaps);
        assertEquals(before.predictions.get(1).gaps, after.predictions.get(1).gaps);
        assertSame(heldOut, history.get(0));
        assertEquals(30, early.id);
        // Reversing dates, rather than just input order/IDs, must change sequential predictions.
        early.date = new Date(2);
        later.date = new Date(1);
        assertNotEquals(before.predictions.get(0).gaps.get("BT"), evaluate(history, 3).predictions.get(0).gaps.get("BT"));
    }

    @Test
    public void separatesPlayerContributionsFromSignedPairsUsingAnalyticExample() {
        Game training = dated(1, 1, team(1, 2), team(3, 4), 2, 1);
        Game heldOut = dated(2, 2, team(1, 2), team(3, 4), 2, 1);
        ExperimentalRatingComparison.Report report = evaluate(Arrays.asList(heldOut, training), 2);
        Map<String, Double> gaps = report.predictions.get(0).gaps;
        // Ridge residual is 1/(1+4/10+2) = 5/17; pair budgets do not bind.
        assertEquals(2.0 / 17, gaps.get("Player-component"), 1e-9);
        assertEquals(10.0 / 17, gaps.get("Pair-component"), 1e-9);
        assertEquals(12.0 / 17, gaps.get("TeamStrength"), 1e-9);
        assertEquals(5.0 / 17, report.marginMae.get("TeamStrength"), 1e-9);
        assertEquals(15.0 / 17, report.marginMae.get("Player-component"), 1e-9);
        assertFalse(report.directionAccuracy.containsKey("Pair-component"));
        assertEquals(2, report.marginMae.size());
    }

    @Test
    public void neutralPredictionsGetHalfCreditAndDrawOnlyHoldoutsHaveNoDirectionMetric() {
        Game training = dated(1, 1, team(1), team(2), 0, 0);
        Game heldOut = dated(2, 2, team(3), team(4), 1, 0);
        ExperimentalRatingComparison.Report report = evaluate(Arrays.asList(training, heldOut), 2);
        for (double accuracy : report.directionAccuracy.values()) assertEquals(0.5, accuracy, 0);
        heldOut.team2Score = 1;
        assertTrue(evaluate(Arrays.asList(training, heldOut), 2).directionAccuracy.isEmpty());
    }

    @Test
    public void filtersSportsAndDeletedGamesBeforeFittingAndScoring() {
        Game training = dated(1, 1, team(1), team(2), 10, 3);
        Game heldOut = dated(2, 2, team(2), team(1), 0, 10);
        Game other = dated(3, 1, team(2), team(1), 99, 0);
        other.sport = Sports.ULTIMATE;
        Game deleted = dated(4, 2, team(2), team(1), 99, 0);
        deleted.deleted = true;
        ExperimentalRatingComparison.Report report = evaluate(Arrays.asList(training, heldOut, other, deleted), 2);
        assertEquals(1, report.trainingGames);
        assertEquals(1, report.heldOutGames);
        assertEquals(evaluate(Arrays.asList(training, heldOut), 2).predictions.get(0).gaps,
                report.predictions.get(0).gaps);
    }

    @Test
    public void syntheticRotatingRostersComparisonIsNotRealPredictiveEvidence() {
        List<Game> games = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            games.add(dated(3 * i + 1, 3 * i + 1, team(1, 2), team(3, 4), 10, 4));
            games.add(dated(3 * i + 2, 3 * i + 2, team(1, 3), team(2, 4), 10, 8));
            games.add(dated(3 * i + 3, 3 * i + 3, team(1, 4), team(2, 3), 6, 10));
        }
        games.add(dated(19, 19, team(1, 2), team(3, 4), 10, 4));
        games.add(dated(20, 20, team(1, 3), team(2, 4), 10, 8));
        games.add(dated(21, 21, team(1, 4), team(2, 3), 6, 10));
        games.add(dated(22, 22, team(1, 5), team(2, 6), 10, 9));
        games.add(dated(23, 23, team(1, 3), team(2, 4), 5, 5));
        ExperimentalRatingComparison.Report report = evaluate(games, 19);
        assertEquals(18, report.trainingGames);
        assertEquals(5, report.heldOutGames);
        assertEquals(1, report.heldOutDraws);
        assertEquals(8, report.directionAccuracy.size());
        assertEquals(report.predictions.get(0).gaps.get("BT"), report.predictions.get(0).gaps.get("PL"), 1e-9);
        assertNotEquals(report.predictions.get(0).gaps.get("BT-margin"), report.predictions.get(0).gaps.get("PL-margin"));
        System.out.println("SYNTHETIC COMPARISON " + new Gson().toJson(report));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyTrainingRatherThanReportingUntrainedModelsAsEvidence() {
        evaluate(Collections.singletonList(dated(1, 2, team(1), team(2), 1, 0)), 2);
    }

    private static ExperimentalRatingComparison.Report evaluate(List<Game> games, long cutoff) {
        return ExperimentalRatingComparison.evaluate(games, Sports.BASKETBALL, new Date(cutoff), 2);
    }

    private static Game dated(long id, long date, List<Player> first, List<Player> second, int a, int b) {
        Game game = game(first, second, a, b);
        game.id = id;
        game.date = new Date(date);
        return game;
    }
}
