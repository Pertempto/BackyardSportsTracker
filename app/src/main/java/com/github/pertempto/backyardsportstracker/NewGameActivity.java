package com.github.pertempto.backyardsportstracker;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.SupportActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class NewGameActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewGameActivity";

    public static final String ARG_GROUPING_ID = "groupingId";

    private DataViewModel dataViewModel;

    private String sport;
    private ArrayList<ArrayList<Player>> teams;

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

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        sport = null;
        teams = new ArrayList<>();
        teams.add(new ArrayList<Player>());
        teams.add(new ArrayList<Player>());

        Spinner spinner = findViewById(R.id.sportChoices);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String sport : Sports.sports) {
            arrayAdapter.add(getString(Sports.names.get(sport)));
        }
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "spinner item selected");
                sport = Sports.sports.get(position);
                for (Player player: teams.get(0)) {
                    if (!player.ratings.containsKey(sport)) {
                        teams.get(0).remove(player);
                    }
                }
                for (Player player: teams.get(1)) {
                    if (!player.ratings.containsKey(sport)) {
                        teams.get(1).remove(player);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sport = "";
                teams.get(0).clear();
                teams.get(1).clear();
                updateTeams();
            }
        });

        // create game from grouping
        if (groupingId != 0) {
            Log.d(LOG_TAG, String.format("creating game from grouping #%d", groupingId));
            final SupportActivity activity = this;
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    Grouping grouping = dataViewModel.getGrouping(groupingId);
                    sport = grouping.sport;
                    teams.get(0).addAll(grouping.team1);
                    teams.get(1).addAll(grouping.team2);
                    Log.d(LOG_TAG, String.format("team1: %s", teams.get(0)));
                    Log.d(LOG_TAG, String.format("team2: %s", teams.get(1)));
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(LOG_TAG, "updating teams");
                            updateTeams();
                        }
                    });
                }
            }).execute();
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
            final SupportActivity activity = this;
            final int finalTeamIndex = teamIndex;
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final List<Player> players = dataViewModel.getAllPlayersBySport(sport);
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
                        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_add_player, null);

                        final Spinner playerSpinner = dialogView.findViewById(R.id.players);
                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item);
                        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                        Collections.sort(players, new Comparator<Player>() {
                            @Override
                            public int compare(Player o1, Player o2) {
                                return Double.compare(o2.ratings.get(sport), o1.ratings.get(sport));
                            }
                        });
                        for (Player player : players) {
                            arrayAdapter.add(String.format(getString(R.string.nameAndRatingFormat), player, player.ratings.get(sport)));
                        }
                        playerSpinner.setAdapter(arrayAdapter);

                        builder.setView(dialogView)
                                .setTitle(R.string.addPlayer)
                                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        Player player = players.get(playerSpinner.getSelectedItemPosition());
                                        if (teams.get(0).contains(player) || teams.get(1).contains(player)) {
                                            Toast.makeText(activity, "Player is already on a team", Toast.LENGTH_SHORT).show();
                                        } else {
                                            teams.get(finalTeamIndex).add(player);
                                            updateTeams();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                builder.show();
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
                Log.d(LOG_TAG, String.format("team 1 score: %d", team1Score));
                Log.d(LOG_TAG, String.format("team 2 score: %d", team2Score));

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
                Toast.makeText(this, "Invalid score", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid teams", Toast.LENGTH_SHORT).show();
        }

    }

    // update the teams lists
    void updateTeams() {
        LinearLayout team1 = findViewById(R.id.team1);
        team1.removeAllViews();
        double team1Rating = 0;
        for (final Player player : teams.get(0)) {
            Log.d(LOG_TAG, String.format("Player on team1: %s", player));
            team1Rating += player.ratings.get(sport);
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, player.ratings.get(sport)));
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
        double team2Rating = 0;
        for (final Player player : teams.get(1)) {
            Log.d(LOG_TAG, String.format("Player on team2: %s", player));
            team2Rating += player.ratings.get(sport);
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, player.ratings.get(sport)));
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
        if ((team1Rating + team2Rating) == 0) {
            team1RatingText.setText("");
            team2RatingText.setText("");
        } else {
            team1RatingText.setText(String.format(getString(R.string.teamRatingAndChanceFormat), team1Rating, team1Rating / (team1Rating + team2Rating) * 100));
            team2RatingText.setText(String.format(getString(R.string.teamRatingAndChanceFormat), team2Rating, team2Rating / (team1Rating + team2Rating) * 100));
        }
    }
}
