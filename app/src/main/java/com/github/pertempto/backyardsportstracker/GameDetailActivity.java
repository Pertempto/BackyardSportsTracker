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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.text.SimpleDateFormat;
import java.util.List;

public class GameDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "GameDetailActivity";
    private static final int REQUEST_EDIT_GAME = 1;

    public static final String ARG_GAME_ID = "gameId";

    private DataViewModel dataViewModel;
    private Game game;
    private long gameId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        gameId = getIntent().getLongExtra(ARG_GAME_ID, 0);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        loadGame();
    }

    private void loadGame() {
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final Game game = dataViewModel.getGame(gameId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setGame(game);
                    }
                });
            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionEdit:
                if (game != null) {
                    Intent intent = new Intent(this, NewGameActivity.class);
                    intent.putExtra(NewGameActivity.ARG_GAME_ID, game.id);
                    startActivityForResult(intent, REQUEST_EDIT_GAME);
                }
                return true;
            case R.id.actionDelete:
                deleteGame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.detail_options, menu);
        menu.findItem(R.id.actionEdit).setVisible(game != null);
        menu.findItem(R.id.actionDelete).setVisible(false);
        if (game == null) {
            return true;
        }
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                List<Game> games = dataViewModel.getAllGamesBySport(game.sport);
                // show delete option for last game only
                if (game.equals(games.get(games.size()-1))) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            menu.findItem(R.id.actionDelete).setVisible(true);
                        }
                    });
                }
            }
        }).execute();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_GAME && resultCode == RESULT_OK) {
            loadGame();
        }
    }

    private void deleteGame() {
        final Activity activity = this;
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                List<Game> games = dataViewModel.getAllGamesBySport(game.sport);
                if (! game.equals(games.get(games.size()-1))) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Can only delete most recent game", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                            .setTitle("Confirmation")
                            .setMessage("Are you sure you to delete this game?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finish();
                                    for (Player player: game.team1) {
                                        player.ratings.put(game.sport, game.initialRatings.get(player.id));
                                        dataViewModel.update(player);
                                    }
                                    for (Player player: game.team2) {
                                        player.ratings.put(game.sport, game.initialRatings.get(player.id));
                                        dataViewModel.update(player);
                                    }
                                    dataViewModel.delete(game);
                                }
                            })
                            .setNegativeButton("Cancel", null);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialogBuilder.show();
                        }
                    });
                }
            }
        }).execute();
    }

    private void setGame(final Game game) {
        this.game = game;

        if (game != null) {
            TextView nameText = findViewById(R.id.name);
            SimpleDateFormat fmt = new SimpleDateFormat(this.getString(R.string.gameDateFormat));
            nameText.setText(fmt.format(game.date));

            TextView sportText = findViewById(R.id.sport);
            sportText.setText(String.format(getString(R.string.sportFormat), getString(Sports.names.get(game.sport))));

            TextView scoreText = findViewById(R.id.score);
            scoreText.setText(String.format(getString(R.string.scoreFormat), game.team1Score, game.team2Score));

            final LinearLayout team1 = findViewById(R.id.team1);
            team1.removeAllViews();
            final LinearLayout team2 = findViewById(R.id.team2);
            team2.removeAllViews();

            double team1Rating = 0;
            for (Player player: game.team1) {
                team1Rating += game.initialRatings.get(player.id);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, game.initialRatings.get(player.id)));
                team1.addView(textView);
            }
            double team2Rating = 0;
            for (Player player: game.team2) {
                team2Rating += game.initialRatings.get(player.id);
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setTextSize(16);
                textView.setText(String.format(getString(R.string.nameAndRatingFormat), player.name, game.initialRatings.get(player.id)));
                team2.addView(textView);
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
        } else {
            Toast.makeText(this, R.string.gameUnavailable, Toast.LENGTH_SHORT).show();
            finish();
        }
        invalidateOptionsMenu();
    }
}
