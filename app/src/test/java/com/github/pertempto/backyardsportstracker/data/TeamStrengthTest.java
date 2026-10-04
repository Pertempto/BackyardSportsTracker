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
    public void fitsPlayersAndOnlySameSideSignedPairEffects() {
        Game game = game(Sports.ULTIMATE, 2, 1, team(10, 30, 50), team(20, 40, 60));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(game), Sports.ULTIMATE);
        // Budget is inactive here: r = 1/(1 + 6/10 + 6/1).
        // Players are 1 +/- r/10; same-side pair effects are +/- r.
        assertEquals(1 + 1.0 / 76, strength.forTeam(team(30)), 1e-9);
        assertEquals(2 + 3.0 / 19, strength.forTeam(team(50, 10)), 1e-9);
        assertEquals(2 - 3.0 / 19, strength.forTeam(team(60, 20)), 1e-9);
        assertEquals(2.0, strength.forTeam(team(10, 40)), 1e-9);
        assertEquals(2 + 1.0 / 76, strength.forTeam(team(99, 10)), 1e-9);
    }

    @Test
    public void fitsSharedPlayerBudgetJointlyRatherThanClippingEachPair() {
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(
                game(Sports.ULTIMATE, 2, 1, team(10), team(20, 30, 40))), Sports.ULTIMATE);
        // All three losing budgets bind. Symmetry gives pair b=-v/4 and team strength
        // 2.25v; minimizing the resulting quadratic yields v=1760/2041, winner=2401/2041.
        double losingPlayer = 1760.0 / 2041;
        assertEquals(2401.0 / 2041, strength.forTeam(team(10)), 1e-8);
        assertEquals(losingPlayer, strength.forTeam(team(20)), 1e-8);
        assertEquals(-440.0 / 2041,
                strength.forTeam(team(20, 40)) - strength.forTeam(team(20))
                        - strength.forTeam(team(40)), 1e-8);
        assertEquals(3960.0 / 2041, strength.forTeam(team(20, 30, 40)), 1e-8);
        assertEquals(losingPlayer / 2,
                strength.forTeam(team(20, 30, 40)) - strength.forTeam(team(30, 40)), 1e-8);
    }

    @Test
    public void ignoresDeletedGamesAndOtherSports() {
        Game valid = game(Sports.ULTIMATE, 2, 1, team(1, 2, 3), team(4, 5, 6));
        Game deleted = game(Sports.ULTIMATE, 99, 0, team(1, 2), team(4, 5));
        deleted.deleted = true;
        Game basketball = game(Sports.BASKETBALL, 0, 99, team(1, 2), team(4, 5));
        TeamStrength strength = TeamStrength.fromGames(Arrays.asList(deleted, basketball, valid),
                Sports.ULTIMATE);
        assertEquals(2 + 3.0 / 19, strength.forTeam(team(2, 1)), 1e-9);
        assertEquals(2 - 3.0 / 19, strength.forTeam(team(5, 4)), 1e-9);
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
        // Repeated evidence: r = 1/(1 + 2*(6/10 + 6)); player change = 2r/10,
        // and each same-side pair effect is +/- 2r, still within the shared budget.
        assertEquals(1 + 1.0 / 71, strength.forTeam(team(1)), 1e-9);
        assertEquals(2 + 12.0 / 71, strength.forTeam(team(2, 3)), 1e-9);
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
