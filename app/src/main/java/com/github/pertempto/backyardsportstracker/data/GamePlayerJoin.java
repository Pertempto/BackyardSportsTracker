package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(tableName = "game_player_join",
        primaryKeys = {"gameId", "playerId"},
        foreignKeys = {
                @ForeignKey(entity = GameEntity.class,
                        parentColumns = "id",
                        childColumns = "gameId"),
                @ForeignKey(entity = PlayerEntity.class,
                        parentColumns = "id",
                        childColumns = "playerId")
        })
public class GamePlayerJoin {
    long gameId;
    long playerId;
    int teamNum;

    public GamePlayerJoin(long gameId, long playerId, int teamNum) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.teamNum = teamNum;
    }
}
