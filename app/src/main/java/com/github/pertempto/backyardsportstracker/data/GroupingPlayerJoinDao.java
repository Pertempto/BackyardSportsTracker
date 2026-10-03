package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GroupingPlayerJoinDao {
    @Insert
    void insert(GroupingPlayerJoin groupingPlayerJoin);

    @Query("SELECT * FROM grouping_player_join")
    List<GroupingPlayerJoin> getAll();

    @Query("DELETE FROM grouping_player_join")
    void deleteAll();

    @Query("SELECT * FROM players INNER JOIN grouping_player_join ON players.id=grouping_player_join.playerId WHERE grouping_player_join.groupingId=:groupingId AND grouping_player_join.teamNum=1")
    List<PlayerEntity> getTeam1Players(long groupingId);

    @Query("SELECT * FROM players INNER JOIN grouping_player_join ON players.id=grouping_player_join.playerId WHERE grouping_player_join.groupingId=:groupingId AND grouping_player_join.teamNum=2")
    List<PlayerEntity> getTeam2Players(long groupingId);
}
