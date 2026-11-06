package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Frozen chronological holdout comparison, never used by production screens or writes. */
public final class ExperimentalRatingComparison {
    public static final class Prediction {
        public long gameId;
        public double actualMargin;
        // Only TeamStrength and its player component are in score-point units.
        public Map<String, Double> gaps = new LinkedHashMap<>();
    }

    public static final class Report {
        public int trainingGames;
        public int heldOutGames;
        public int heldOutDraws;
        public List<Prediction> predictions = new ArrayList<>();
        // Decisive games only; neutral predictions receive half credit.
        public Map<String, Double> directionAccuracy = new LinkedHashMap<>();
        public Map<String, Double> marginMae = new LinkedHashMap<>();
    }

    private ExperimentalRatingComparison() {}

    /** All games before cutoff train; games at/after cutoff are never used for fitting. */
    public static Report evaluate(List<Game> games, String sport, Date cutoff, double margin) {
        if (!Sports.sports.contains(sport) || cutoff == null
                || Double.isNaN(margin) || Double.isInfinite(margin) || margin < 0) {
            throw new IllegalArgumentException("Require one sport, a cutoff, and a nonnegative margin.");
        }
        List<Game> selected = new ArrayList<>();
        for (Game game : games) {
            if (!game.deleted && sport.equals(game.sport)) {
                ExperimentalOpenSkill.validateGame(game);
                if (game.date == null) {
                    throw new IllegalArgumentException("Games need dates for chronological evaluation.");
                }
                selected.add(game);
            }
        }
        Collections.sort(selected, new Comparator<Game>() {
            @Override
            public int compare(Game first, Game second) {
                int byDate = first.date.compareTo(second.date);
                return byDate != 0 ? byDate : Long.compare(first.id, second.id);
            }
        });
        List<Game> training = new ArrayList<>();
        List<Game> heldOut = new ArrayList<>();
        Map<Long, Double> scalar = new HashMap<>();
        Map<String, Map<Long, ExperimentalOpenSkill.Rating>> beliefs = new LinkedHashMap<>();
        beliefs.put("BT", new HashMap<Long, ExperimentalOpenSkill.Rating>());
        beliefs.put("BT-margin", new HashMap<Long, ExperimentalOpenSkill.Rating>());
        beliefs.put("PL", new HashMap<Long, ExperimentalOpenSkill.Rating>());
        beliefs.put("PL-margin", new HashMap<Long, ExperimentalOpenSkill.Rating>());
        for (Game game : selected) {
            if (!game.date.before(cutoff)) {
                heldOut.add(game);
                continue;
            }
            // Neutral scalar priors, not current Player.ratings or held-out initialRatings.
            HashMap<Long, Double> snapshot = new HashMap<>();
            for (List<Player> team : java.util.Arrays.asList(game.team1, game.team2)) {
                for (Player player : team) {
                    snapshot.put(player.id, scalar.containsKey(player.id) ? scalar.get(player.id) : 1000);
                }
            }
            Game copy = new Game(sport, game.date, snapshot, game.team1Score, game.team2Score,
                    game.team1, game.team2);
            // TrueSkill's normal API uses ID order. Copies give it chronological order
            // for this evaluation without changing app history semantics or caller IDs.
            copy.id = training.size() + 1;
            training.add(copy);
            double factor1 = copy.getTeam1ChangeFactor();
            double factor2 = copy.getTeam2ChangeFactor();
            for (Player player : copy.team1) {
                scalar.put(player.id, snapshot.get(player.id) * factor1);
            }
            for (Player player : copy.team2) {
                scalar.put(player.id, snapshot.get(player.id) * factor2);
            }
            for (String name : beliefs.keySet()) {
                beliefs.put(name, ExperimentalOpenSkill.rate(copy, beliefs.get(name),
                        name.startsWith("BT") ? ExperimentalOpenSkill.Model.BRADLEY_TERRY
                                : ExperimentalOpenSkill.Model.PLACKETT_LUCE,
                        name.endsWith("margin") ? margin : 0, Collections.<Long, Double>emptyMap()));
            }
        }
        if (training.isEmpty() || heldOut.isEmpty()) {
            throw new IllegalArgumentException("Need training and held-out games on opposite sides of cutoff.");
        }
        TeamStrength strength = TeamStrength.fromGames(training, sport);
        ExperimentalTrueSkill trueSkill = ExperimentalTrueSkill.fromGames(training, sport);
        Report report = new Report();
        report.trainingGames = training.size();
        report.heldOutGames = heldOut.size();
        for (Game game : heldOut) {
            Prediction prediction = new Prediction();
            prediction.gameId = game.id;
            prediction.actualMargin = (double) game.team1Score - game.team2Score;
            prediction.gaps.put("Scalar", scalarTeam(scalar, game.team1) - scalarTeam(scalar, game.team2));
            double full = strength.forTeam(game.team1) - strength.forTeam(game.team2);
            double players = playerComponent(strength, game.team1) - playerComponent(strength, game.team2);
            prediction.gaps.put("TeamStrength", full);
            prediction.gaps.put("Player-component", players);
            // A diagnostic contribution, not a standalone player rating or predictor.
            prediction.gaps.put("Pair-component", full - players);
            double trueSkillGap = 0;
            for (Player player : game.team1) trueSkillGap += trueSkill.rating(player.id).mean;
            for (Player player : game.team2) trueSkillGap -= trueSkill.rating(player.id).mean;
            prediction.gaps.put("TrueSkill", trueSkillGap);
            for (String name : beliefs.keySet()) {
                prediction.gaps.put(name, ExperimentalOpenSkill.forTeam(beliefs.get(name), game.team1)
                        - ExperimentalOpenSkill.forTeam(beliefs.get(name), game.team2));
            }
            report.predictions.add(prediction);
            if (prediction.actualMargin == 0) report.heldOutDraws++;
        }
        for (String name : report.predictions.get(0).gaps.keySet()) {
            if (name.equals("Pair-component")) continue;
            double correct = 0;
            double absoluteError = 0;
            for (Prediction prediction : report.predictions) {
                double gap = prediction.gaps.get(name);
                if (prediction.actualMargin != 0) {
                    correct += Math.abs(gap) < 1e-9 ? 0.5
                            : (Math.signum(gap) == Math.signum(prediction.actualMargin) ? 1 : 0);
                }
                absoluteError += Math.abs(gap - prediction.actualMargin);
            }
            if (report.heldOutGames > report.heldOutDraws) {
                report.directionAccuracy.put(name, correct / (report.heldOutGames - report.heldOutDraws));
            }
            if (name.equals("TeamStrength") || name.equals("Player-component")) {
                report.marginMae.put(name, absoluteError / report.heldOutGames);
            }
        }
        return report;
    }

    private static double playerComponent(TeamStrength model, List<Player> team) {
        double total = 0;
        for (Player player : team) total += model.forTeam(Collections.singletonList(player));
        return total;
    }

    private static double scalarTeam(Map<Long, Double> ratings, List<Player> team) {
        double total = 0;
        for (Player player : team) total += ratings.containsKey(player.id) ? ratings.get(player.id) : 1000;
        return total;
    }
}
