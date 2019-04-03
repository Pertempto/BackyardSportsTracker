package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Entity;

@Entity(tableName = "groupings")
public class GroupingEntity extends BaseEntity {
    public String sport;

    public GroupingEntity(String sport) {
        this.sport = sport;
    }
}
