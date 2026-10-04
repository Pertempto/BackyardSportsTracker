package com.github.pertempto.backyardsportstracker.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TeamStrengthTest {
    @Test
    public void fitsNonnegativePlayersAndOnlySameSidePairBonuses() {
        Game game = game(Sports.ULTIMATE, 2, 1, team(10, 30, 50), team(20, 40, 60));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(game), Sports.ULTIMATE);
        // Residual r = 1/(1 + 6/10 + 3/1). Players are 1 +/- r/10;
        // winning pairs get r, while losing pairs stay at their nonnegative boundary of 0.
        assertEquals(1 + 1.0 / 46, strength.forTeam(team(30)), 1e-9);
        assertEquals(2 + 6.0 / 23, strength.forTeam(team(50, 10)), 1e-9);
        assertEquals(2 - 1.0 / 23, strength.forTeam(team(60, 20)), 1e-9);
        assertEquals(2.0, strength.forTeam(team(10, 40)), 1e-9);
        assertEquals(2 + 1.0 / 46, strength.forTeam(team(99, 10)), 1e-9);
    }

    @Test
    public void ignoresDeletedGamesAndOtherSports() {
        Game valid = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        Game deleted = game(Sports.ULTIMATE, 99, 0, team(1, 2), team(4, 5));
        deleted.deleted = true;
        Game basketball = game(Sports.BASKETBALL, 0, 99, team(1, 2), team(4, 5));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(deleted, basketball, valid),
                Sports.ULTIMATE);
        assertEquals(2 + 6.0 / 23, strength.forTeam(team(2, 1)), 1e-9);
        assertEquals(2 - 1.0 / 23, strength.forTeam(team(5, 4)), 1e-9);
    }

    @Test
    public void weightsGamesEquallyRegardlessOfDateScoreTotalAndInputOrder() {
        Game win = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        win.date = new Date(0);
        Game loss = game(Sports.ULTIMATE, 100, 101, team(1, 2, 3), team(4, 5, 6));
        loss.date = new Date(999999999);
        // Same features with opposite raw margins cancel exactly, despite different score totals.
        assertEquals(2.0, TeamStrength.fromGames(Arrays.asList(win, loss), Sports.ULTIMATE)
                .forTeam(team(1, 2)), 1e-12);
        assertEquals(2.0, TeamStrength.fromGames(Arrays.asList(loss, win), Sports.ULTIMATE)
                .forTeam(team(2, 1)), 1e-12);
    }

    @Test
    public void repeatedAndSideSwappedGamesFitWithoutSingularities() {
        Game first = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        Game swapped = game(Sports.ULTIMATE, 1, 2, team(6, 5, 4), team(3, 2, 1));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(first, swapped), Sports.ULTIMATE);
        // Repeated evidence: r = 1/(1 + 2*(6/10 + 3)); player change = 2r/10,
        // and each winning pair bonus is 2r.
        assertEquals(1 + 1.0 / 41, strength.forTeam(team(1)), 1e-9);
        assertEquals(2 + 12.0 / 41, strength.forTeam(team(2, 3)), 1e-9);
    }

    @Test
    public void hasNoTeamOffsetOrOutputClampAndNeverSubtractsTeammates() {
        TeamStrength empty = TeamStrength.fromGames(Collections.<Game>emptyList(), Sports.ULTIMATE);
        assertEquals(0.0, empty.forTeam(Collections.<Player>emptyList()), 1e-12);
        assertEquals(1.0, empty.forTeam(team(99)), 1e-12);
        assertEquals(3.0, empty.forTeam(team(1, 2, 3)), 1e-12);
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(
                game(Sports.ULTIMATE, 99, 0, team(1), team(2))), Sports.ULTIMATE);
        // With the losing weight at 0, the winner solves (w-99) + 10*(w-1) = 0.
        assertEquals(109.0 / 11, strength.forTeam(team(1)), 1e-9);
        assertEquals(0.0, strength.forTeam(team(2)), 1e-9);
        assertEquals(109.0 / 11, strength.forTeam(team(1, 2)), 1e-9);
        assertEquals(109.0 / 11 + 1, strength.forTeam(team(1, 99)), 1e-9);
        assertEquals(1.0, strength.forTeam(team(99)), 1e-12);
    }

    private static Game game(String sport, int scored, int conceded,
                             List<Player> first, List<Player> second) {
        return new Game(sport, new Date(0), new HashMap<Long, Double>(), scored, conceded, first, second);
    }

    private static List<Player> team(long... ids) {
        Player[] players = new Player[ids.length];
        for (int i = 0; i < ids.length; i++) {
            players[i] = new Player("Player " + ids[i], new HashMap<String, Double>());
            players[i].id = ids[i];
        }
        return Arrays.asList(players);
    }
}
