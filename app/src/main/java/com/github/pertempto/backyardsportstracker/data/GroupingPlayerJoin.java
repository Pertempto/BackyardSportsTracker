package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(tableName = "grouping_player_join",
        primaryKeys = {"groupingId", "playerId"},
        foreignKeys = {
                @ForeignKey(entity = GroupingEntity.class,
                        parentColumns = "id",
                        childColumns = "groupingId"),
                @ForeignKey(entity = PlayerEntity.class,
                        parentColumns = "id",
                        childColumns = "playerId")
        })
public class GroupingPlayerJoin {
    long groupingId;
    long playerId;
    int teamNum;

    public GroupingPlayerJoin(long groupingId, long playerId, int teamNum) {
        this.groupingId = groupingId;
        this.playerId = playerId;
        this.teamNum = teamNum;
    }
}
