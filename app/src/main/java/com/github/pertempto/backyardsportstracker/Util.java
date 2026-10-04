package com.github.pertempto.backyardsportstracker;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.TeamStrength;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        final TeamStrength strength = TeamStrength.fromGames(games, sport);

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
                    return Double.compare(strength.forTeam(o1), strength.forTeam(o2));
                }
            });
            groupings.add(new Grouping(sport, teams.get(0), teams.get(1)));
        }

        Collections.sort(groupings, new Comparator<Grouping>() {
            @Override
            public int compare(Grouping o1, Grouping o2) {
                return Double.compare(rateGrouping(o1, strength), rateGrouping(o2, strength));
            }
        });

        return groupings;
    }

    public static double teamStrength(List<Player> team, List<Game> games, String sport) {
        return TeamStrength.fromGames(games, sport).forTeam(team);
    }

    private static double rateGrouping(Grouping grouping, TeamStrength strength) {
        return Math.abs(strength.forTeam(grouping.team1) - strength.forTeam(grouping.team2));
    }
}
