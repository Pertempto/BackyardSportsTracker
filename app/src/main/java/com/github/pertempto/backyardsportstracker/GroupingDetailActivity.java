package com.github.pertempto.backyardsportstracker;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.List;

public class GroupingDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "GroupingDetailActivity";
    private static final int REQUEST_EDIT_GROUPING = 1;

    public static final String ARG_GROUPING_ID = "groupingId";

    private DataViewModel dataViewModel;
    private Grouping grouping;
    private long groupingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grouping_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        groupingId = getIntent().getLongExtra(ARG_GROUPING_ID, 0);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        loadGrouping();
    }

    private void loadGrouping() {
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final Grouping grouping = dataViewModel.getGrouping(groupingId);
                final List<Game> games = dataViewModel.getAllGames();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setGrouping(grouping, games);
                    }
                });
            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionEdit:
                if (grouping == null) {
                    Toast.makeText(this, R.string.groupingUnavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Log.d(LOG_TAG, "grouping edit");
                Intent editIntent = new Intent(this, NewGroupingActivity.class);
                editIntent.putExtra(ARG_GROUPING_ID, grouping.id);
                editIntent.putExtra(NewGameActivity.ARG_INITIAL_SPORT, grouping.sport);
                startActivityForResult(editIntent, REQUEST_EDIT_GROUPING);
                return true;
            case R.id.actionDelete:
                deleteGrouping();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_options, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_GROUPING && resultCode == RESULT_OK) {
            loadGrouping();
        }
    }

    public void onCreateGameClick(View view) {
        if (grouping == null) {
            Toast.makeText(this, R.string.groupingUnavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, NewGameActivity.class);
        Log.d(LOG_TAG, String.format("creating new game with grouping #%d", grouping.id));
        intent.putExtra(NewGameActivity.ARG_GROUPING_ID, grouping.id);
        intent.putExtra(NewGameActivity.ARG_INITIAL_SPORT, grouping.sport);
        // start the new game activity
        startActivity(intent);
        // close this activity
        finish();
    }

    private void deleteGrouping() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you to delete this grouping?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                        dataViewModel.delete(grouping);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setGrouping(final Grouping grouping, List<Game> games) {
        this.grouping = grouping;

        if (grouping != null) {
            TextView nameText = findViewById(R.id.name);
            nameText.setText(grouping.getName(this));

            TextView sportText = findViewById(R.id.sport);
            sportText.setText(String.format(getString(R.string.sportFormat), getString(Sports.names.get(grouping.sport))));

            final LinearLayout team1 = findViewById(R.id.team1);
            team1.removeAllViews();
            final LinearLayout team2 = findViewById(R.id.team2);
            team2.removeAllViews();

            PlayerStats.sortPlayers(grouping.team1, games, grouping.sport);
            for (Player player:grouping.team1) {
                PlayerStats stats = PlayerStats.fromGames(player, games, grouping.sport);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(getString(R.string.nameAndPointsFormat, player.name, stats.pointsFor, stats.pointsAgainst));
                team1.addView(textView);
            }
            PlayerStats.sortPlayers(grouping.team2, games, grouping.sport);
            for (Player player:grouping.team2) {
                PlayerStats stats = PlayerStats.fromGames(player, games, grouping.sport);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(getString(R.string.nameAndPointsFormat, player.name, stats.pointsFor, stats.pointsAgainst));
                team2.addView(textView);
            }

            TextView team1RatingText = findViewById(R.id.team1Rating);
            TextView team2RatingText = findViewById(R.id.team2Rating);
            PlayerStats stats1 = PlayerStats.fromTeam(grouping.team1, games, grouping.sport);
            PlayerStats stats2 = PlayerStats.fromTeam(grouping.team2, games, grouping.sport);
            team1RatingText.setText(getString(R.string.pointsRecordFormat, stats1.pointsFor, stats1.pointsAgainst));
            team2RatingText.setText(getString(R.string.pointsRecordFormat, stats2.pointsFor, stats2.pointsAgainst));
            findViewById(R.id.team1ExpectedScore).setVisibility(View.GONE);
            findViewById(R.id.team2ExpectedScore).setVisibility(View.GONE);
        }
    }
}
