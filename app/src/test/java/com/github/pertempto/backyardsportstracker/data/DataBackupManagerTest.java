package com.github.pertempto.backyardsportstracker.data;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class DataBackupManagerTest {
    @Test
    public void backupJsonRoundTripsDatabaseRowsAndRosterLinks() {
        DataBackup backup = new DataBackup();

        PlayerEntity player = new PlayerEntity("Alex", new HashMap<String, Double>());
        player.id = 42;
        player.ratings.put("basketball", 1200.0);
        backup.players.add(player);

        HashMap<Long, Double> initialRatings = new HashMap<>();
        initialRatings.put(player.id, 1200.0);
        GameEntity game = new GameEntity("basketball", new Date(1234), initialRatings, 11, 7);
        game.id = 13;
        backup.games.add(game);
        backup.gamePlayers.add(new GamePlayerJoin(game.id, player.id, 1));

        GroupingEntity grouping = new GroupingEntity("basketball");
        grouping.id = 27;
        backup.groupings.add(grouping);
        backup.groupingPlayers.add(new GroupingPlayerJoin(grouping.id, player.id, 1));

        DataBackup restored = new Gson().fromJson(new Gson().toJson(backup), DataBackup.class);
        DataBackupManager.validate(restored);

        assertEquals(42L, restored.players.get(0).id);
        assertEquals(1200.0, restored.games.get(0).initialRatings.get(42L), 0.0);
        assertEquals(1, restored.gamePlayers.get(0).teamNum);
        assertEquals(27L, restored.groupingPlayers.get(0).groupingId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBackupWithRosterLinkToMissingPlayer() {
        DataBackup backup = new DataBackup();
        GameEntity game = new GameEntity("basketball", new Date(1234), new HashMap<Long, Double>(), 11, 7);
        game.id = 13;
        backup.games.add(game);
        backup.gamePlayers.add(new GamePlayerJoin(game.id, 42, 1));

        DataBackupManager.validate(backup);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedBackupVersion() {
        DataBackup backup = new DataBackup();
        backup.formatVersion = 2;

        DataBackupManager.validate(backup);
    }
}
