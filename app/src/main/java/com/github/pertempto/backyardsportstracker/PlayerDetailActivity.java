package com.github.pertempto.backyardsportstracker;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class PlayerDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "PlayerDetailActivity";

    public static final String ARG_PLAYER_ID = "playerId";

    private DataViewModel dataViewModel;
    private long playerId;
    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate player detail activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        playerId = getIntent().getLongExtra(ARG_PLAYER_ID, 0);
        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionEdit:
                editPlayer();
                return true;
            case R.id.actionDelete:
                deletePlayer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // refresh player, might have changed in edit activity
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final Player player = dataViewModel.getPlayer(playerId);
                final List<Game> games = dataViewModel.getPlayerGames(playerId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPlayer(player, games);
                    }
                });
            }
        }).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_options, menu);
        return true;
    }

    private void editPlayer() {
        Intent intent = new Intent(this, PlayerEditActivity.class);
        intent.putExtra(PlayerEditActivity.ARG_PLAYER_ID, player.id);
        startActivity(intent);
    }

    private void deletePlayer() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you to delete this player?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                        dataViewModel.delete(player);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setPlayer(final Player player, List<Game> games) {
        this.player = player;

        if (player != null) {
            TextView textView = findViewById(R.id.name);
            textView.setText(player.name);

            PlayerStats stats = PlayerStats.fromGames(player, games, null);
            TextView pointsFor = findViewById(R.id.pointsFor);
            pointsFor.setText(getString(R.string.pointsForFormat, stats.pointsFor));
            TextView pointsAgainst = findViewById(R.id.pointsAgainst);
            pointsAgainst.setText(getString(R.string.pointsAgainstFormat, stats.pointsAgainst));

            LinearLayout teammatesList = findViewById(R.id.teammates);
            teammatesList.removeAllViews();
            final HashMap<Player, PlayerStats> pairs = PlayerStats.withTeammates(player, games);
            List<Player> teammates = new ArrayList<>(pairs.keySet());
            Collections.sort(teammates, new Comparator<Player>() {
                @Override
                public int compare(Player first, Player second) {
                    int difference = Long.compare(pairs.get(second).getDifference(),
                            pairs.get(first).getDifference());
                    return difference != 0 ? difference : first.name.compareToIgnoreCase(second.name);
                }
            });
            if (teammates.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText(R.string.noTeammates);
                teammatesList.addView(empty);
            }
            for (Player teammate : teammates) {
                PlayerStats pair = pairs.get(teammate);
                TextView row = new TextView(this);
                row.setText(getString(R.string.nameAndPointsFormat,
                        teammate.name, pair.pointsFor, pair.pointsAgainst));
                row.setTextSize(16);
                row.setPadding(0, 8, 0, 8);
                teammatesList.addView(row);
            }

            final LinearLayout gamesList = findViewById(R.id.games);
            gamesList.removeAllViews();
            Collections.reverse(games);
            for (Game game: games) {
                final ConstraintLayout gameRow = (ConstraintLayout) getLayoutInflater().inflate(R.layout.list_row, gamesList, false);
                ImageView icon = gameRow.findViewById(R.id.icon);
                icon.setImageResource(Sports.icons.get(game.sport));
                TextView nameView = gameRow.findViewById(R.id.name);
                nameView.setText(game.getName(this));
                TextView detailView = gameRow.findViewById(R.id.detail);
                detailView.setText(game.getPlayerResult(player, this));
                gamesList.addView(gameRow);
            }
        }
    }
}
