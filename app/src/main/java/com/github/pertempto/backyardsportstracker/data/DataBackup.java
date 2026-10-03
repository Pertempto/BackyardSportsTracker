package com.github.pertempto.backyardsportstracker.data;

import java.util.ArrayList;
import java.util.List;

public class DataBackup {
    public int formatVersion = 1;
    public List<PlayerEntity> players = new ArrayList<>();
    public List<GameEntity> games = new ArrayList<>();
    public List<GroupingEntity> groupings = new ArrayList<>();
    public List<GamePlayerJoin> gamePlayers = new ArrayList<>();
    public List<GroupingPlayerJoin> groupingPlayers = new ArrayList<>();
}
