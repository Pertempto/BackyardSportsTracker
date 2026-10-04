package com.github.pertempto.backyardsportstracker;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Util {

    private static final String LOG_TAG = "Util";

    /* Use Html.fromHtml correctly. Based off of https://stackoverflow.com/a/37905107 */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    public static ArrayList<Grouping> generateGroupings(ArrayList<Player> players, final String sport,
                                                       List<Game> games) {
        PlayerStats.sortPlayers(players, games, sport);
        final HashMap<Long, PlayerStats> stats = new HashMap<>();
        for (Player player : players) {
            stats.put(player.id, PlayerStats.fromGames(player, games, sport));
        }
        final HashMap<Long, HashMap<Long, Integer>> pairGames = new HashMap<>();
        for (Player player : players) {
            pairGames.put(player.id, new HashMap<Long, Integer>());
        }
        for (Game game : games) {
            if (game.deleted || (sport != null && !sport.equals(game.sport))) {
                continue;
            }
            countPairGames(game.team1, pairGames);
            countPairGames(game.team2, pairGames);
        }

        ArrayList<Grouping> groupings = new ArrayList<>();
        if (players.size() == 0) {
            Grouping grouping = new Grouping(sport, new ArrayList<Player>(), new ArrayList<Player>());
            groupings.add(grouping);
            return groupings;
        } else if (players.size() == 1) {
            ArrayList<Player> team2 = new ArrayList<>();
            team2.add(players.get(0));
            Grouping grouping = new Grouping(sport, new ArrayList<Player>(), team2);
            groupings.add(grouping);
            return groupings;
        }

        for (int i=1; i < Math.pow(2, players.size()-1); i++) {
            ArrayList<ArrayList<Player>> teams = new ArrayList<>();
            teams.add(new ArrayList<Player>());
            teams.add(new ArrayList<Player>());

            for (int j=0; j < players.size(); j++) {
                Player player = players.get(j);
                int teamIndex = (int) Math.floor((i % Math.pow(2, j + 1)) / Math.pow(2, j));
                teams.get(teamIndex).add(player);
            }
            Collections.sort(teams, new Comparator<ArrayList<Player>>() {
                @Override
                public int compare(ArrayList<Player> o1, ArrayList<Player> o2) {
                    return Double.compare(rateTeam(o1, stats), rateTeam(o2, stats));
                }
            });
            groupings.add(new Grouping(sport, teams.get(0), teams.get(1)));
        }

        Collections.sort(groupings, new Comparator<Grouping>() {
            @Override
            public int compare(Grouping o1, Grouping o2) {
                return Double.compare(rateGrouping(o1, stats, pairGames),
                        rateGrouping(o2, stats, pairGames));
            }
        });

        return groupings;
    }

    private static void countPairGames(List<Player> team,
                                       HashMap<Long, HashMap<Long, Integer>> pairGames) {
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                long first = Math.min(team.get(i).id, team.get(j).id);
                long second = Math.max(team.get(i).id, team.get(j).id);
                if (!pairGames.containsKey(first) || !pairGames.containsKey(second)) {
                    continue;
                }
                HashMap<Long, Integer> counts = pairGames.get(first);
                Integer count = counts.get(second);
                counts.put(second, count == null ? 1 : count + 1);
            }
        }
    }

    private static long familiarity(List<Player> team,
                                    HashMap<Long, HashMap<Long, Integer>> pairGames) {
        long total = 0;
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                long first = Math.min(team.get(i).id, team.get(j).id);
                long second = Math.max(team.get(i).id, team.get(j).id);
                Integer count = pairGames.get(first).get(second);
                if (count != null) {
                    total += count;
                }
            }
        }
        return total;
    }

    private static double rateGrouping(Grouping grouping, HashMap<Long, PlayerStats> stats,
                                       HashMap<Long, HashMap<Long, Integer>> pairGames) {
        return Math.abs(rateTeam(grouping.team1, stats) - rateTeam(grouping.team2, stats))
                + 0.01 * (familiarity(grouping.team1, pairGames)
                + familiarity(grouping.team2, pairGames));
    }

    private static double rateTeam(List<Player> team, HashMap<Long, PlayerStats> stats) {
        double strength = 0;
        for (Player player: team) {
            strength += stats.get(player.id).getStrength();
        }
        return strength;
    }
}
