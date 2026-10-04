package com.github.pertempto.backyardsportstracker;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NewGameActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewGameActivity";

    public static final String ARG_GROUPING_ID = "groupingId";
    public static final String ARG_GAME_ID = "gameId";
    public static final String ARG_INITIAL_SPORT = "initialSport";

    private DataViewModel dataViewModel;

    private String sport;
    private ArrayList<ArrayList<Player>> teams;
    private Game editingGame;
    private List<Game> games = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final long groupingId = getIntent().getLongExtra(ARG_GROUPING_ID, 0);
        final long gameId = getIntent().getLongExtra(ARG_GAME_ID, 0);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        String initialSport = getIntent().getStringExtra(ARG_INITIAL_SPORT);
        if (!Sports.sports.contains(initialSport)) {
            initialSport = SportPreferences.getSelectedSport(this);
        } else {
            SportPreferences.setSelectedSport(this, initialSport);
        }
        sport = initialSport;
        teams = new ArrayList<>();
        teams.add(new ArrayList<Player>());
        teams.add(new ArrayList<Player>());

        final Spinner spinner = findViewById(R.id.sportChoices);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String sport : Sports.sports) {
            arrayAdapter.add(getString(Sports.names.get(sport)));
        }
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(Sports.sports.indexOf(sport), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "spinner item selected");
                sport = Sports.sports.get(position);
                SportPreferences.setSelectedSport(NewGameActivity.this, sport);
                for (Iterator<Player> players = teams.get(0).iterator(); players.hasNext();) {
                    Player player = players.next();
                    if (!player.ratings.containsKey(sport)) {
                        players.remove();
                    }
                }
                for (Iterator<Player> players = teams.get(1).iterator(); players.hasNext();) {
                    Player player = players.next();
                    if (!player.ratings.containsKey(sport)) {
                        players.remove();
                    }
                }
                updateTeams();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sport = "";
                teams.get(0).clear();
                teams.get(1).clear();
                updateTeams();
            }
        });

        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final List<Game> history = dataViewModel.getAllGames();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        games = history;
                        updateTeams();
                    }
                });
            }
        }).execute();

        if (gameId != 0) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.editGame);
            }
            spinner.setEnabled(false);
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final Game game = dataViewModel.getGame(gameId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (game == null || game.deleted) {
                                Toast.makeText(NewGameActivity.this, R.string.gameUnavailable, Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            editingGame = game;
                            sport = game.sport;
                            SportPreferences.setSelectedSport(NewGameActivity.this, sport);
                            spinner.setSelection(Sports.sports.indexOf(sport), false);
                            ((EditText) findViewById(R.id.team1Score)).setText(String.valueOf(game.team1Score));
                            ((EditText) findViewById(R.id.team2Score)).setText(String.valueOf(game.team2Score));
                            teams.get(0).addAll(game.team1);
                            teams.get(1).addAll(game.team2);
                            updateTeams();
                        }
                    });
                }
            }).execute();
        }

        // create game from grouping
        if (groupingId != 0) {
            Log.d(LOG_TAG, String.format("creating game from grouping #%d", groupingId));
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final Grouping grouping = dataViewModel.getGrouping(groupingId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (grouping == null) {
                                Toast.makeText(NewGameActivity.this, R.string.groupingUnavailable, Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            teams.get(0).addAll(grouping.team1);
                            teams.get(1).addAll(grouping.team2);
                            filterTeamsForSport();
                            Log.d(LOG_TAG, String.format("team1: %s", teams.get(0)));
                            Log.d(LOG_TAG, String.format("team2: %s", teams.get(1)));
                            Log.d(LOG_TAG, "updating teams");
                            updateTeams();
                        }
                    });
                }
            }).execute();
        }
    }

    private void filterTeamsForSport() {
        for (ArrayList<Player> team : teams) {
            for (Iterator<Player> players = team.iterator(); players.hasNext();) {
                if (!players.next().ratings.containsKey(sport)) {
                    players.remove();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.submit_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSubmit:
                submitGame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onAddPlayerClick(View view) {
        Log.d(LOG_TAG, "onAddPlayerClick");
        int teamIndex = -1;
        if (view.getId() == R.id.addPlayerTeam1) {
            teamIndex = 0;
        } else if (view.getId() == R.id.addPlayerTeam2) {
            teamIndex = 1;
        }
        if (sport == null) {
            Toast.makeText(this, "Choose a sport first", Toast.LENGTH_SHORT).show();
        } else {
            final NewGameActivity activity = this;
            final int finalTeamIndex = teamIndex;
            final String selectedSport = sport;
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final List<Player> players = dataViewModel.getAllPlayersBySport(selectedSport);
                    players.removeAll(teams.get(0));
                    players.removeAll(teams.get(1));
                    if (players.size() == 0) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "No more players for this sport", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PlayerSelectionDialog.show(activity, players, selectedSport, games,
                                        new PlayerSelectionDialog.OnPlayersSelectedListener() {
                                            @Override
                                            public void onPlayersSelected(ArrayList<Player> selectedPlayers) {
                                                teams.get(finalTeamIndex).addAll(selectedPlayers);
                                                updateTeams();
                                            }
                                        });
                            }
                        });
                    }
                }
            }).execute();
        }
    }

    void submitGame() {
        Log.d(LOG_TAG, "submit game");
        if (teams.get(0).size() > 0 && teams.get(1).size() > 0) {
            EditText team1ScoreEdit = findViewById(R.id.team1Score);
            EditText team2ScoreEdit = findViewById(R.id.team2Score);
            try {
                int team1Score = Integer.parseInt(team1ScoreEdit.getText().toString());
                int team2Score = Integer.parseInt(team2ScoreEdit.getText().toString());
                if (team1Score < 0 || team2Score < 0 || (long) team1Score + team2Score == 0) {
                    Toast.makeText(this, R.string.invalidScore, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(LOG_TAG, String.format("team 1 score: %d", team1Score));
                Log.d(LOG_TAG, String.format("team 2 score: %d", team2Score));

                if (editingGame != null) {
                    editingGame.team1Score = team1Score;
                    editingGame.team2Score = team2Score;
                    editingGame.team1 = new ArrayList<>(teams.get(0));
                    editingGame.team2 = new ArrayList<>(teams.get(1));
                    dataViewModel.update(editingGame, new Runnable() {
                        @Override
                        public void run() {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                    return;
                }

                Date date = new Date();
                HashMap<Long, Double> initialRatings = new HashMap<>();
                for (Player player: teams.get(0)) {
                    initialRatings.put(player.id, player.ratings.get(sport));
                }
                for (Player player: teams.get(1)) {
                    initialRatings.put(player.id, player.ratings.get(sport));
                }

                Game game = new Game(sport, date, initialRatings, team1Score, team2Score, teams.get(0), teams.get(1));

                double team1Factor = game.getTeam1ChangeFactor();
                double team2Factor = game.getTeam2ChangeFactor();
                Log.d(LOG_TAG, String.format("team 1 factor: %f", team1Factor));
                Log.d(LOG_TAG, String.format("team 2 factor: %f", team2Factor));

                for (int i = 0; i < teams.get(0).size(); i++) {
                    Player player = teams.get(0).get(i);
                    player.ratings.put(sport, player.ratings.get(sport) * team1Factor);
                    dataViewModel.update(player);
                }

                for (int i = 0; i < teams.get(1).size(); i++) {
                    Player player = teams.get(1).get(i);
                    player.ratings.put(sport, player.ratings.get(sport) * team2Factor);
                    dataViewModel.update(player);
                }
                dataViewModel.insert(game);

                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.invalidScore, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid teams", Toast.LENGTH_SHORT).show();
        }

    }

    // update the teams lists
    void updateTeams() {
        LinearLayout team1 = findViewById(R.id.team1);
        team1.removeAllViews();
        PlayerStats.sortPlayers(teams.get(0), games, sport);
        for (final Player player : teams.get(0)) {
            Log.d(LOG_TAG, String.format("Player on team1: %s", player));
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(player.name);
            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    teams.get(0).remove(player);
                    updateTeams();
                }
            });
            team1.addView(row);
        }

        LinearLayout team2 = findViewById(R.id.team2);
        team2.removeAllViews();
        PlayerStats.sortPlayers(teams.get(1), games, sport);
        for (final Player player : teams.get(1)) {
            Log.d(LOG_TAG, String.format("Player on team2: %s", player));
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(player.name);
            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    teams.get(1).remove(player);
                    updateTeams();
                }
            });
            team2.addView(row);
        }

        TextView team1RatingText = findViewById(R.id.team1Rating);
        TextView team2RatingText = findViewById(R.id.team2Rating);
        double strength1 = Util.teamStrength(teams.get(0), games, sport);
        double strength2 = Util.teamStrength(teams.get(1), games, sport);
        team1RatingText.setGravity(Gravity.CENTER);
        team2RatingText.setGravity(Gravity.CENTER);
        team1RatingText.setText(getString(R.string.teamStrengthFormat, strength1));
        team2RatingText.setText(getString(R.string.teamStrengthFormat, strength2));
    }
}
