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
import java.util.Iterator;
import java.util.List;

public class NewGroupingActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewGroupingActivity";

    private DataViewModel dataViewModel;

    private String sport;
    private ArrayList<Player> selectedPlayers;
    private ArrayList<Grouping> groupings;
    private int groupingIndex = 0;
    private long editingGroupingId;
    private List<Game> games = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_grouping);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        final long groupingId = getIntent().getLongExtra(GroupingDetailActivity.ARG_GROUPING_ID, 0);
        editingGroupingId = groupingId;
        String initialSport = getIntent().getStringExtra(NewGameActivity.ARG_INITIAL_SPORT);
        if (!Sports.sports.contains(initialSport)) {
            initialSport = SportPreferences.getSelectedSport(this);
        } else {
            SportPreferences.setSelectedSport(this, initialSport);
        }
        sport = initialSport;
        selectedPlayers = new ArrayList<>();
        updateGroupings();

        if (editingGroupingId != 0 && actionBar != null) {
            actionBar.setTitle(R.string.editGrouping);
        }

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
                SportPreferences.setSelectedSport(NewGroupingActivity.this, sport);
                for (Iterator<Player> players = selectedPlayers.iterator(); players.hasNext();) {
                    Player player = players.next();
                    if (!player.ratings.containsKey(sport)) {
                        players.remove();
                    }
                }
                updateGroupings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sport = "";
                selectedPlayers.clear();
                updateGroupings();
            }
        });

        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final List<Game> history = dataViewModel.getAllGames();
                    final Grouping grouping = editingGroupingId == 0 ? null : dataViewModel.getGrouping(editingGroupingId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            games = history;
                            if (editingGroupingId == 0) {
                                updateGroupings();
                                return;
                            }
                            if (grouping == null) {
                                Toast.makeText(NewGroupingActivity.this, R.string.groupingUnavailable, Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            sport = grouping.sport;
                            SportPreferences.setSelectedSport(NewGroupingActivity.this, sport);
                            spinner.setSelection(Sports.sports.indexOf(sport), false);
                            selectedPlayers.clear();
                            selectedPlayers.addAll(grouping.team1);
                            selectedPlayers.addAll(grouping.team2);
                            groupings.clear();
                            groupings.add(grouping);
                            groupingIndex = 0;
                            updateTeams();
                        }
                    });
                }
            }).execute();
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
                submitGrouping();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onLastGroupingClick(View view) {
        groupingIndex--;
        groupingIndex = (groupingIndex + groupings.size()) % groupings.size();
        updateTeams();
    }

    public void onNextGroupingClick(View view) {
        groupingIndex++;
        groupingIndex %= groupings.size();
        updateTeams();
    }

    public void onAddPlayerClick(View view) {
        Log.d(LOG_TAG, "onAddPlayerClick");
        if (sport == null) {
            Toast.makeText(this, "Choose a sport first", Toast.LENGTH_SHORT).show();
        } else {
            final NewGroupingActivity activity = this;
            final String selectedSport = sport;
            new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
                @Override
                public void call() {
                    final List<Player> players = dataViewModel.getAllPlayersBySport(selectedSport);
                    final List<Player> team1Players = getGrouping().team1;
                    final List<Player> team2Players = getGrouping().team2;
                    players.removeAll(team1Players);
                    players.removeAll(team2Players);
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
                                            public void onPlayersSelected(ArrayList<Player> playersToAdd) {
                                                selectedPlayers.addAll(playersToAdd);
                                                updateGroupings();
                                            }
                                        });
                            }
                        });
                    }
                }
            }).execute();
        }
    }

    void submitGrouping() {
        Log.d(LOG_TAG, "submit grouping");
        if (getGrouping().team1.size() > 0 && getGrouping().team2.size() > 0) {
            Grouping grouping = getGrouping();
            if (editingGroupingId == 0) {
                dataViewModel.insert(grouping);
                finish();
            } else {
                grouping.id = editingGroupingId;
                dataViewModel.update(grouping, new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        } else {
            Toast.makeText(this, "Invalid teams", Toast.LENGTH_SHORT).show();
        }

    }

    // update the list of possible groupings
    void updateGroupings() {
        groupings = Util.generateGroupings(selectedPlayers, sport, games);
        groupingIndex = 0;
        updateTeams();
    }

    // update the team view lists
    void updateTeams() {
        TextView groupingText = findViewById(R.id.groupingText);
        groupingText.setText(String.format("(%d/%d)", groupingIndex + 1, groupings.size()));
        LinearLayout team1 = findViewById(R.id.team1);
        team1.removeAllViews();
        PlayerStats.sortPlayers(getGrouping().team1, games, sport);
        for (final Player player : getGrouping().team1) {
            PlayerStats stats = PlayerStats.fromGames(player, games, sport);
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(getString(R.string.nameAndPointsFormat, player.name, stats.pointsFor, stats.pointsAgainst));
            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPlayers.remove(player);
                    updateGroupings();
                }
            });
            team1.addView(row);
        }

        LinearLayout team2 = findViewById(R.id.team2);
        team2.removeAllViews();
        PlayerStats.sortPlayers(getGrouping().team2, games, sport);
        for (final Player player : getGrouping().team2) {
            PlayerStats stats = PlayerStats.fromGames(player, games, sport);
            View row = getLayoutInflater().inflate(R.layout.deletable_item, null);
            TextView textView = row.findViewById(R.id.text);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextSize(16);
            textView.setText(getString(R.string.nameAndPointsFormat, player.name, stats.pointsFor, stats.pointsAgainst));
            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPlayers.remove(player);
                    updateGroupings();
                }
            });
            team2.addView(row);
        }

        TextView team1RatingText = findViewById(R.id.team1Rating);
        TextView team2RatingText = findViewById(R.id.team2Rating);
        PlayerStats stats1 = PlayerStats.fromTeam(getGrouping().team1, games, sport);
        PlayerStats stats2 = PlayerStats.fromTeam(getGrouping().team2, games, sport);
        team1RatingText.setText(getString(R.string.pointsRecordFormat, stats1.pointsFor, stats1.pointsAgainst));
        team2RatingText.setText(getString(R.string.pointsRecordFormat, stats2.pointsFor, stats2.pointsAgainst));
        findViewById(R.id.team1ExpectedScore).setVisibility(View.GONE);
        findViewById(R.id.team2ExpectedScore).setVisibility(View.GONE);
    }

    private Grouping getGrouping() {
        return groupings.get(groupingIndex);
    }
}
