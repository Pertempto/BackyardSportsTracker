package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Update;

public interface DataDao<T> {
    @Insert
    long insert(T obj);

    @Update
    void update(T obj);
}
