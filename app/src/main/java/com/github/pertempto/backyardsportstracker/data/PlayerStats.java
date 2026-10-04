package com.github.pertempto.backyardsportstracker.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class PlayerStats {
    public int wins;
    public int losses;
    public int ties;
    public long pointsFor;
    public long pointsAgainst;

    public long getDifference() {
        return pointsFor - pointsAgainst;
    }

    public double getStrength() {
        double totalPoints = pointsFor + (double) pointsAgainst;
        return totalPoints == 0 ? 0.5 : pointsFor / totalPoints;
    }

    public static HashMap<Player, PlayerStats> withTeammates(Player player, List<Game> games) {
        HashMap<Player, PlayerStats> pairs = new HashMap<>();
        for (Game game : games) {
            if (game.deleted) {
                continue;
            }
            List<Player> team;
            int scored;
            int conceded;
            if (game.team1.contains(player)) {
                team = game.team1;
                scored = game.team1Score;
                conceded = game.team2Score;
            } else if (game.team2.contains(player)) {
                team = game.team2;
                scored = game.team2Score;
                conceded = game.team1Score;
            } else {
                continue;
            }
            for (Player teammate : team) {
                if (teammate.equals(player)) {
                    continue;
                }
                PlayerStats stats = pairs.get(teammate);
                if (stats == null) {
                    stats = new PlayerStats();
                    pairs.put(teammate, stats);
                }
                stats.addScore(scored, conceded);
            }
        }
        return pairs;
    }

    public static PlayerStats fromTeam(List<Player> team, List<Game> games, String sport) {
        PlayerStats total = new PlayerStats();
        for (Player player : team) {
            PlayerStats stats = fromGames(player, games, sport);
            total.pointsFor += stats.pointsFor;
            total.pointsAgainst += stats.pointsAgainst;
        }
        return total;
    }

    public static void sortPlayers(List<Player> players, List<Game> games, String sport) {
        final HashMap<Long, PlayerStats> stats = new HashMap<>();
        for (Player player : players) {
            stats.put(player.id, fromGames(player, games, sport));
        }
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player first, Player second) {
                int difference = Long.compare(stats.get(second.id).getDifference(),
                        stats.get(first.id).getDifference());
                return difference != 0 ? difference : first.name.compareToIgnoreCase(second.name);
            }
        });
    }

    public static PlayerStats fromGames(Player player, List<Game> games, String sport) {
        PlayerStats stats = new PlayerStats();
        for (Game game : games) {
            if (game.deleted || (sport != null && !sport.equals(game.sport))) {
                continue;
            }
            int scored;
            int conceded;
            if (game.team1.contains(player)) {
                scored = game.team1Score;
                conceded = game.team2Score;
            } else if (game.team2.contains(player)) {
                scored = game.team2Score;
                conceded = game.team1Score;
            } else {
                continue;
            }
            stats.addScore(scored, conceded);
        }
        return stats;
    }

    private void addScore(int scored, int conceded) {
        pointsFor += scored;
        pointsAgainst += conceded;
        if (scored > conceded) {
            wins++;
        } else if (scored < conceded) {
            losses++;
        } else {
            ties++;
        }
    }
}
