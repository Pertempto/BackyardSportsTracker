package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Entity;

import java.util.Date;
import java.util.HashMap;

@Entity(tableName = "games")
public class GameEntity extends BaseEntity {
    public String sport;
    public Date date;
    public HashMap<Long, Double> initialRatings;
    public int team1Score;
    public int team2Score;

    public GameEntity(String sport, Date date, HashMap<Long, Double> initialRatings, int team1Score, int team2Score) {
        this.sport = sport;
        this.date = date;
        this.initialRatings = initialRatings;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }
}
