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

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

public class GroupingDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "GroupingDetailActivity";

    public static final String ARG_GROUPING_ID = "groupingId";

    private DataViewModel dataViewModel;
    private Grouping grouping;

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

        final long groupingId = getIntent().getLongExtra(ARG_GROUPING_ID, 0);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final Grouping grouping = dataViewModel.getGrouping(groupingId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setGrouping(grouping);
                    }
                });
            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionEdit:
                Log.d(LOG_TAG, "grouping edit");
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

    public void onCreateGameClick(View view) {
        Intent intent = new Intent(this, NewGameActivity.class);
        Log.d(LOG_TAG, String.format("creating new game with grouping #%d", grouping.id));
        intent.putExtra(NewGameActivity.ARG_GROUPING_ID, grouping.id);
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

    private void setGrouping(final Grouping grouping) {
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

            double team1Rating = 0;
            for (Player player:grouping.team1) {
                team1Rating += player.ratings.get(grouping.sport);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, player.ratings.get(grouping.sport)));
                team1.addView(textView);
            }
            double team2Rating = 0;
            for (Player player:grouping.team2) {
                team2Rating += player.ratings.get(grouping.sport);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, player.ratings.get(grouping.sport)));
                team2.addView(textView);
            }

            TextView team1RatingText = findViewById(R.id.team1Rating);
            TextView team2RatingText = findViewById(R.id.team2Rating);
            TextView team1ExpectedScoreText = findViewById(R.id.team1ExpectedScore);
            TextView team2ExpectedScoreText = findViewById(R.id.team2ExpectedScore);
            if ((team1Rating + team2Rating) == 0) {
                team1RatingText.setText("");
                team2RatingText.setText("");
                team1ExpectedScoreText.setText("");
                team2ExpectedScoreText.setText("");
            } else {
                team1RatingText.setText(String.format(getString(R.string.teamRatingAndChanceFormat), team1Rating, team1Rating / (team1Rating + team2Rating) * 100));
                team2RatingText.setText(String.format(getString(R.string.teamRatingAndChanceFormat), team2Rating, team2Rating / (team1Rating + team2Rating) * 100));
                double team1ExpectedScore = 0;
                double team2ExpectedScore = 0;
                if (team1Rating > team2Rating) {
                    team1ExpectedScore = Sports.targetScores.get(grouping.sport);
                    team2ExpectedScore = Sports.targetScores.get(grouping.sport) * (team2Rating/team1Rating);
                } else {
                    team2ExpectedScore = Sports.targetScores.get(grouping.sport);
                    team1ExpectedScore = Sports.targetScores.get(grouping.sport) * (team1Rating/team2Rating);
                }
                team1ExpectedScoreText.setText(String.format(getString(R.string.expectedScoreFormat), team1ExpectedScore));
                team2ExpectedScoreText.setText(String.format(getString(R.string.expectedScoreFormat), team2ExpectedScore));
            }
        }
    }
}
