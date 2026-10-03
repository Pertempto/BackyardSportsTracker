package com.github.pertempto.backyardsportstracker.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GamePlayerJoinDao {
    @Insert
    void insert(GamePlayerJoin gamePlayerJoin);

    @Query("SELECT * FROM game_player_join")
    List<GamePlayerJoin> getAll();

    @Query("DELETE FROM game_player_join")
    void deleteAll();

    @Query("DELETE FROM game_player_join WHERE gameId = :gameId")
    void deleteAllForGame(long gameId);

    @Query("SELECT * FROM players INNER JOIN game_player_join ON players.id=game_player_join.playerId WHERE game_player_join.gameId=:gameId AND game_player_join.teamNum=1")
    List<PlayerEntity> getTeam1Players(long gameId);

    @Query("SELECT * FROM players INNER JOIN game_player_join ON players.id=game_player_join.playerId WHERE game_player_join.gameId=:gameId AND game_player_join.teamNum=2")
    List<PlayerEntity> getTeam2Players(long gameId);

    @Query("SELECT games.* FROM games INNER JOIN game_player_join ON games.id=game_player_join.gameId WHERE game_player_join.playerId=:playerId AND games.deleted != 1 ORDER BY games.id ASC")
    List<GameEntity> getPlayerGames(long playerId);
}
