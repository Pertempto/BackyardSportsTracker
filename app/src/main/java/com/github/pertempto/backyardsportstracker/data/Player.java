package com.github.pertempto.backyardsportstracker.data;

import android.support.annotation.NonNull;

import java.util.HashMap;

public class Player extends BaseDataObject {
    public String name;
    public HashMap<String, Double> ratings;

    public Player(String name, HashMap<String, Double> ratings) {
        super();
        this.name = name;
        this.ratings = ratings;
    }

    public PlayerEntity toEntity() {
        PlayerEntity entity = new PlayerEntity(name, ratings);
        entity.id = id;
        entity.deleted = deleted;
        return entity;
    }

    @NonNull
    @Override
    public String toString() {
        return this.name;
    }
}
