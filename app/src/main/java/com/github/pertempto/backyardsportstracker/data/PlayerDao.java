package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface PlayerDao extends DataDao<PlayerEntity> {

    @Query("SELECT * FROM players WHERE deleted != 1 ORDER BY id ASC")
    List<PlayerEntity> getAll();

    @Query("SELECT * FROM players ORDER BY id ASC")
    List<PlayerEntity> getAllIncludingDeleted();

    @Query("DELETE FROM players")
    void deleteAll();

    @Query("SELECT * FROM players WHERE id = :id")
    PlayerEntity getById(long id);

//    @Query("SELECT * FROM players WHERE id IN (:ids)")
//    LiveData<List<PlayerEntity>> getByIds(int[] ids);

}
