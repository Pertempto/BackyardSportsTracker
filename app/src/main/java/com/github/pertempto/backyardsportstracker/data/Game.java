package com.github.pertempto.backyardsportstracker.data;

import android.content.Context;

import com.github.pertempto.backyardsportstracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Game extends BaseDataObject{
    private static final String LOG_TAG = "Game";

    public String sport;
    public Date date;
    public HashMap<Long, Double> initialRatings;
    public List<Player> team1;
    public List<Player> team2;
    public int team1Score;
    public int team2Score;

    public Game(String sport, Date date, HashMap<Long, Double> initialRatings, int team1Score, int team2Score, List<Player> team1, List<Player> team2) {
        super();
        this.sport = sport;
        this.date = date;
        this.initialRatings = initialRatings;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.team1 = team1;
        this.team2 = team2;

    }

    public GameEntity toEntity() {
        GameEntity entity = new GameEntity(sport, date, initialRatings, team1Score, team2Score);
        entity.id = id;
        entity.deleted = deleted;
        return entity;
    }

    public String getName(Context context) {
        SimpleDateFormat fmt = new SimpleDateFormat(context.getString(R.string.gameDateFormat));
        return fmt.format(date);
    }

    public String getPlayerResult(Player player, Context context) {
        if (team1.contains(player)) {
            StringBuilder result = new StringBuilder();
            if (team1Score > team2Score) {
                result.append(context.getString(R.string.won));
            } else if (team1Score == team2Score) {
                result.append(context.getString(R.string.tie));
            } else {
                result.append(context.getString(R.string.lost));
            }
            result.append(" ");
            result.append(String.format(context.getString(R.string.scoreFormat), team1Score, team2Score));
            return result.toString();
        } else if (team2.contains(player)) {
            StringBuilder result = new StringBuilder();
            if (team2Score > team1Score) {
                result.append(context.getString(R.string.won));
            } else if (team2Score == team1Score) {
                result.append(context.getString(R.string.tie));
            } else {
                result.append(context.getString(R.string.lost));
            }
            result.append(" ");
            result.append(String.format(context.getString(R.string.scoreFormat), team2Score, team1Score));
            return result.toString();
        }
        return "Did not play";
    }

    public double getTeam1ChangeFactor() {
        return getChangeFactors().get(0);
    }

    public double getTeam2ChangeFactor() {
        return getChangeFactors().get(1);
    }

    private ArrayList<Double> getChangeFactors() {
        // TODO: create getTeam1Rating and getTeam2Rating methods
        double team1Rating = 0;
        for (Player player: team1) {
            team1Rating += initialRatings.get(player.id);
        }
        double team2Rating = 0;
        for (Player player: team2) {
            team2Rating += initialRatings.get(player.id);
        }

        double totalRating = team1Rating + team2Rating;
        long totalScore = (long) team1Score + team2Score;
        if (team1Rating <= 0 || team2Rating <= 0 || totalRating <= 0
                || team1Score < 0 || team2Score < 0 || totalScore <= 0) {
            ArrayList<Double> unchangedFactors = new ArrayList<>();
            unchangedFactors.add(1.0);
            unchangedFactors.add(1.0);
            return unchangedFactors;
        }
        double team1Chance = team1Rating / totalRating;
        double team2Chance = team2Rating / totalRating;
//        Log.d(LOG_TAG, String.format("team 1 chance: %f", team1Chance));
//        Log.d(LOG_TAG, String.format("team 2 chance: %f", team2Chance));
        double team1ScoreRatio = (double)team1Score / totalScore;
        double team2ScoreRatio = (double)team2Score / totalScore;
//        Log.d(LOG_TAG, String.format("team 1 score ratio: %f", team1ScoreRatio));
//        Log.d(LOG_TAG, String.format("team 2 score ratio: %f", team2ScoreRatio));

        double team1Factor = (team1ScoreRatio / team1Chance);
        double team2Factor = (team2ScoreRatio / team2Chance);
        if (team1Factor > 1) {
            team1Factor = (team1Factor - 1) * Sports.changeFactors.get(sport) + 1;
            team2Factor = 1 / team1Factor;
        } else if (team2Factor > 1) {
            team2Factor = (team2Factor - 1) * Sports.changeFactors.get(sport) + 1;
            team1Factor = 1 / team2Factor;
        }
        ArrayList<Double> changeFactors = new ArrayList<>();
        changeFactors.add(team1Factor);
        changeFactors.add(team2Factor);
        return changeFactors;
    }
}
