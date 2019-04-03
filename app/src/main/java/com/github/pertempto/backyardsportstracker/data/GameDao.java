package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GameDao extends DataDao<GameEntity> {

    @Query("SELECT * FROM games WHERE deleted != 1 ORDER BY id ASC")
    List<GameEntity> getAll();

    @Query("SELECT * FROM games WHERE id = :id")
    GameEntity getById(long id);
}
