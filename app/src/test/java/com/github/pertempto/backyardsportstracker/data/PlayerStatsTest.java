package com.github.pertempto.backyardsportstracker.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PlayerStatsTest {
    @Test
    public void strengthUsesOnlyRecordedPointsAndDefaultsToNeutralWithoutPoints() {
        PlayerStats stats = new PlayerStats();
        assertEquals(0.5, stats.getStrength(1), 1e-12);
        assertEquals(2.0, stats.getStrength(4), 1e-12);
        assertEquals(0.0, stats.getStrength(0), 1e-12);
        stats.pointsFor = 3;
        assertEquals(1.0, stats.getStrength(1), 1e-12);
        stats.pointsFor = 0;
        stats.pointsAgainst = 3;
        assertEquals(0.0, stats.getStrength(1), 1e-12);
        stats.pointsFor = 6;
        stats.pointsAgainst = 4;
        assertEquals(0.6, stats.getStrength(1), 1e-12);
        stats.pointsFor = 60;
        stats.pointsAgainst = 40;
        assertEquals(0.6, stats.getStrength(1), 1e-12);
        stats.pointsFor = 31;
        stats.pointsAgainst = 36;
        assertEquals(62.0 / 67, stats.getStrength(2), 1e-12);
        stats.pointsFor = 141;
        stats.pointsAgainst = 145;
        assertEquals(564.0 / 143, stats.getStrength(8), 1e-12);
    }

    @Test
    public void countsBothTeamsAndFiltersSportDeletedGamesAndNonParticipants() {
        Player player = player(1);
        Player teammate = player(2);
        Game win = game(Sports.ULTIMATE, 3, 0, player, teammate);
        // A separately loaded Player object with the same ID still participates.
        Game loss = game(Sports.ULTIMATE, 3, 1, teammate, player(1));
        Game tie = game(Sports.ULTIMATE, 2, 2, player, teammate);
        Game otherSport = game(Sports.BASKETBALL, 10, 4, teammate, player);
        Game deleted = game(Sports.ULTIMATE, 99, 0, player, teammate);
        deleted.deleted = true;
        Game unrelated = game(Sports.ULTIMATE, 80, 70, teammate, player(3));
        List<Game> games = Arrays.asList(win, loss, tie, otherSport, deleted, unrelated);

        PlayerStats sportStats = PlayerStats.fromGames(player, games, Sports.ULTIMATE);
        assertStats(sportStats, 1, 1, 1, 6, 5);
        PlayerStats allStats = PlayerStats.fromGames(player, games, null);
        assertStats(allStats, 1, 2, 1, 10, 15);
    }

    @Test
    public void emptyHistoryHasZeroTotalsAndScoreEditsAreReflected() {
        Player player = player(1);
        assertStats(PlayerStats.fromGames(player, Collections.<Game>emptyList(), null),
                0, 0, 0, 0, 0);
        Game game = game(Sports.ULTIMATE, 3, 0, player, player(2));
        assertStats(PlayerStats.fromGames(player, Arrays.asList(game), null),
                1, 0, 0, 3, 0);
        game.team1Score = 1;
        game.team2Score = 3;
        assertStats(PlayerStats.fromGames(player, Arrays.asList(game), null),
                0, 1, 0, 1, 3);
    }

    @Test
    public void teammateTotalsCountSharedGamesOnceAndExcludeOpponentsAndSoloGames() {
        Player player = player(1);
        Player teammate = player(2);
        Player occasional = player(3);
        Player opponent = player(4);
        Game win = new Game(Sports.ULTIMATE, new Date(0), new HashMap<Long, Double>(), 3, 0,
                Arrays.asList(player, teammate, occasional), Arrays.asList(opponent));
        Game loss = new Game(Sports.BASKETBALL, new Date(0), new HashMap<Long, Double>(), 10, 4,
                Arrays.asList(opponent), Arrays.asList(player(1), player(2)));
        Game apart = game(Sports.ULTIMATE, 3, 1, player, teammate);
        Game deleted = new Game(Sports.ULTIMATE, new Date(0), new HashMap<Long, Double>(), 90, 0,
                Arrays.asList(player, teammate), Arrays.asList(opponent));
        deleted.deleted = true;
        HashMap<Player, PlayerStats> pairs = PlayerStats.withTeammates(player,
                Arrays.asList(win, loss, apart, deleted));

        assertEquals(2, pairs.size());
        assertStats(pairs.get(teammate), 1, 1, 0, 7, 10);
        assertStats(pairs.get(occasional), 1, 0, 0, 3, 0);
        assertFalse(pairs.containsKey(player));
        assertFalse(pairs.containsKey(opponent));
        assertEquals(0, PlayerStats.withTeammates(player, Collections.<Game>emptyList()).size());
    }

    @Test
    public void teamTotalsSumEachPlayersHistoryAndSortByDifferenceNotPointsFor() {
        Player first = player(1);
        Player second = player(2);
        Player opponent = player(3);
        Game shared = new Game(Sports.ULTIMATE, new Date(0), new HashMap<Long, Double>(), 3, 1,
                Arrays.asList(first, second), Arrays.asList(opponent));
        Game extra = game(Sports.ULTIMATE, 10, 12, first, opponent);
        List<Game> games = Arrays.asList(shared, extra);
        PlayerStats team = PlayerStats.fromTeam(Arrays.asList(first, second), games, Sports.ULTIMATE);
        assertEquals(16, team.pointsFor);
        assertEquals(14, team.pointsAgainst);
        assertEquals(2, team.getDifference());

        List<Player> players = Arrays.asList(first, second, opponent);
        PlayerStats.sortPlayers(players, games, Sports.ULTIMATE);
        assertEquals(Arrays.asList(second, first, opponent), players);
        assertEquals(0, PlayerStats.fromTeam(Collections.<Player>emptyList(), games, null).pointsFor);
    }

    private static Player player(long id) {
        Player player = new Player("Player " + id, new HashMap<String, Double>());
        player.id = id;
        return player;
    }

    private static Game game(String sport, int score1, int score2, Player player1, Player player2) {
        return new Game(sport, new Date(0), new HashMap<Long, Double>(), score1, score2,
                Arrays.asList(player1), Arrays.asList(player2));
    }

    private static void assertStats(PlayerStats stats, int wins, int losses, int ties,
                                    long pointsFor, long pointsAgainst) {
        assertEquals(wins, stats.wins);
        assertEquals(losses, stats.losses);
        assertEquals(ties, stats.ties);
        assertEquals(pointsFor, stats.pointsFor);
        assertEquals(pointsAgainst, stats.pointsAgainst);
    }
}
