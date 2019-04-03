package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Entity;

import java.util.HashMap;

@Entity(tableName = "players")
public class PlayerEntity extends BaseEntity {
    public String name;
    public HashMap<String, Double> ratings;

    public PlayerEntity(String name, HashMap<String, Double> ratings) {
        this.name = name;
        this.ratings = ratings;
        this.deleted = false;
    }
}
