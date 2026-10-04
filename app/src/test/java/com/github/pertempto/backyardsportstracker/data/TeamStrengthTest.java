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
    public void fitsPlayersAndOnlySameSidePairsWithUnitRidgePenalty() {
        Game game = game(Sports.ULTIMATE, 2, 1, team(10, 30, 50), team(20, 40, 60));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(game), Sports.ULTIMATE);
        // Six player and six same-side pair indicators: X X' + I = 13.
        // Each winning player/pair coefficient is +1/13; each losing one is -1/13.
        assertEquals(0.5 + 1.0 / 13, strength.forTeam(team(30)), 1e-12);
        assertEquals(1 + 3.0 / 13, strength.forTeam(team(50, 10)), 1e-12);
        assertEquals(1 - 3.0 / 13, strength.forTeam(team(60, 20)), 1e-12);
        assertEquals(1.0, strength.forTeam(team(10, 40)), 1e-12);
        assertEquals(1 + 1.0 / 13, strength.forTeam(team(99, 10)), 1e-12);
    }

    @Test
    public void ignoresDeletedGamesAndOtherSports() {
        Game valid = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        Game deleted = game(Sports.ULTIMATE, 99, 0, team(1, 2), team(4, 5));
        deleted.deleted = true;
        Game basketball = game(Sports.BASKETBALL, 0, 99, team(1, 2), team(4, 5));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(deleted, basketball, valid),
                Sports.ULTIMATE);
        assertEquals(1 + 3.0 / 13, strength.forTeam(team(2, 1)), 1e-12);
        assertEquals(1 - 3.0 / 13, strength.forTeam(team(5, 4)), 1e-12);
    }

    @Test
    public void weightsGamesEquallyRegardlessOfDateScoreTotalAndInputOrder() {
        Game win = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        win.date = new Date(0);
        Game loss = game(Sports.ULTIMATE, 100, 101, team(1, 2, 3), team(4, 5, 6));
        loss.date = new Date(999999999);
        // Same features with opposite raw margins cancel exactly, despite different score totals.
        assertEquals(1.0, TeamStrength.fromGames(Arrays.asList(win, loss), Sports.ULTIMATE)
                .forTeam(team(1, 2)), 1e-12);
        assertEquals(1.0, TeamStrength.fromGames(Arrays.asList(loss, win), Sports.ULTIMATE)
                .forTeam(team(2, 1)), 1e-12);
    }

    @Test
    public void repeatedAndSideSwappedGamesFitWithoutSingularities() {
        Game first = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        Game swapped = game(Sports.ULTIMATE, 1, 2, team(6, 5, 4), team(3, 2, 1));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(first, swapped), Sports.ULTIMATE);
        // Two equivalent rows: the coefficient magnitude is 2/(1 + 2*12) = 2/25.
        assertEquals(0.5 + 2.0 / 25, strength.forTeam(team(1)), 1e-12);
        assertEquals(1 + 6.0 / 25, strength.forTeam(team(2, 3)), 1e-12);
    }

    @Test
    public void boundsStrengthAndTreatsUnseenPlayersAsNeutral() {
        TeamStrength empty = TeamStrength.fromGames(Collections.<Game>emptyList(), Sports.ULTIMATE);
        assertEquals(0.0, empty.forTeam(Collections.<Player>emptyList()), 1e-12);
        assertEquals(0.5, empty.forTeam(team(99)), 1e-12);
        assertEquals(1.5, empty.forTeam(team(1, 2, 3)), 1e-12);
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(
                game(Sports.ULTIMATE, 99, 0, team(1, 2, 3), team(4, 5, 6))), Sports.ULTIMATE);
        assertEquals(2.1, strength.forTeam(team(1, 2, 3)), 1e-12);
        assertEquals(0.9, strength.forTeam(team(4, 5, 6)), 1e-12);
        assertEquals(0.5, strength.forTeam(team(99)), 1e-12);
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
