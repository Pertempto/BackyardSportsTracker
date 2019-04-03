package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.PrimaryKey;

import java.util.Objects;

public class BaseEntity {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @PrimaryKey(autoGenerate = true)
    public long id;

    public boolean deleted;
}
