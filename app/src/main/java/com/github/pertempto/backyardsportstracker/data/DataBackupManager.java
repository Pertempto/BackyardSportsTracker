package com.github.pertempto.backyardsportstracker.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DataBackupManager {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DataBackupManager() {}

    public static String exportJson(Database database) {
        DataBackup backup = new DataBackup();
        backup.players = database.playerDao().getAllIncludingDeleted();
        backup.games = database.gameDao().getAllIncludingDeleted();
        backup.groupings = database.groupingDao().getAllIncludingDeleted();
        backup.gamePlayers = database.gamePlayerJoinDao().getAll();
        backup.groupingPlayers = database.groupingPlayerJoinDao().getAll();
        return GSON.toJson(backup);
    }

    public static void importJson(final Database database, Reader reader) {
        final DataBackup backup = GSON.fromJson(reader, DataBackup.class);
        validate(backup);

        database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                database.gamePlayerJoinDao().deleteAll();
                database.groupingPlayerJoinDao().deleteAll();
                database.gameDao().deleteAll();
                database.groupingDao().deleteAll();
                database.playerDao().deleteAll();

                for (PlayerEntity player : backup.players) {
                    database.playerDao().insert(player);
                }
                for (GameEntity game : backup.games) {
                    database.gameDao().insert(game);
                }
                for (GroupingEntity grouping : backup.groupings) {
                    database.groupingDao().insert(grouping);
                }
                for (GamePlayerJoin join : backup.gamePlayers) {
                    database.gamePlayerJoinDao().insert(join);
                }
                for (GroupingPlayerJoin join : backup.groupingPlayers) {
                    database.groupingPlayerJoinDao().insert(join);
                }
            }
        });
    }

    static void validate(DataBackup backup) {
        if (backup == null || backup.formatVersion != FORMAT_VERSION
                || backup.players == null || backup.games == null || backup.groupings == null
                || backup.gamePlayers == null || backup.groupingPlayers == null) {
            throw new IllegalArgumentException("Unsupported or incomplete backup file.");
        }

        Map<Long, PlayerEntity> playersById = new HashMap<>();
        for (PlayerEntity player : backup.players) {
            if (player == null || player.id <= 0 || player.name == null || player.name.trim().isEmpty()
                    || player.ratings == null || playersById.put(player.id, player) != null) {
                throw new IllegalArgumentException("Invalid player data in backup.");
            }
        }

        Map<Long, GameEntity> gamesById = new HashMap<>();
        for (GameEntity game : backup.games) {
            if (game == null || game.id <= 0 || game.sport == null || game.date == null
                    || game.initialRatings == null || gamesById.put(game.id, game) != null) {
                throw new IllegalArgumentException("Invalid game data in backup.");
            }
            for (Map.Entry<Long, Double> rating : game.initialRatings.entrySet()) {
                if (rating.getKey() == null || !playersById.containsKey(rating.getKey())
                        || rating.getValue() == null) {
                    throw new IllegalArgumentException("Invalid historical ratings in backup.");
                }
            }
        }

        Map<Long, GroupingEntity> groupingsById = new HashMap<>();
        for (GroupingEntity grouping : backup.groupings) {
            if (grouping == null || grouping.id <= 0 || grouping.sport == null
                    || groupingsById.put(grouping.id, grouping) != null) {
                throw new IllegalArgumentException("Invalid grouping data in backup.");
            }
        }

        Set<String> gamePlayerPairs = new HashSet<>();
        for (GamePlayerJoin join : backup.gamePlayers) {
            GameEntity game = join == null ? null : gamesById.get(join.gameId);
            if (join == null || game == null || !playersById.containsKey(join.playerId)
                    || (join.teamNum != 1 && join.teamNum != 2)
                    || game.initialRatings.get(join.playerId) == null
                    || !gamePlayerPairs.add(join.gameId + ":" + join.playerId)) {
                throw new IllegalArgumentException("Invalid game roster data in backup.");
            }
        }

        Set<String> groupingPlayerPairs = new HashSet<>();
        for (GroupingPlayerJoin join : backup.groupingPlayers) {
            GroupingEntity grouping = join == null ? null : groupingsById.get(join.groupingId);
            PlayerEntity player = join == null ? null : playersById.get(join.playerId);
            if (join == null || grouping == null || player == null
                    || (join.teamNum != 1 && join.teamNum != 2)
                    || player.ratings.get(grouping.sport) == null
                    || !groupingPlayerPairs.add(join.groupingId + ":" + join.playerId)) {
                throw new IllegalArgumentException("Invalid grouping roster data in backup.");
            }
        }
    }
}
