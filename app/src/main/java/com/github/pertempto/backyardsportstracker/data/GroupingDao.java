package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GroupingDao extends DataDao<GroupingEntity> {

    @Query("SELECT * FROM groupings WHERE deleted != 1 ORDER BY id ASC")
    List<GroupingEntity> getAll();

    @Query("SELECT * FROM groupings WHERE id = :id")
    GroupingEntity getById(long id);
}
