package com.github.pertempto.backyardsportstracker;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;

public class PlayerEditActivity extends AppCompatActivity {

    private static final String LOG_TAG = "PlayerEditActivity";

    public static final String ARG_PLAYER_ID = "playerId";

    private ArrayList<String> newSports;
    private DataViewModel dataViewModel;
    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        newSports = new ArrayList<>();

        final long playerId = getIntent().getLongExtra(ARG_PLAYER_ID, 0);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final Player player = dataViewModel.getPlayer(playerId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPlayer(player);
                    }
                });
            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSubmit:
                Log.d(LOG_TAG, "player submit");
                submitPlayer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.submit_options, menu);
        return true;
    }

    public void onAddSport(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_sport_rating, null);

        final Spinner spinner = dialogView.findViewById(R.id.sportChoices);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final ArrayList<String> sportChoices = new ArrayList<>();
        for (String sport: Sports.sports) {
            if (!player.ratings.containsKey(sport)) {
                arrayAdapter.add(getString(Sports.names.get(sport)));
                sportChoices.add(sport);
            }
        }
        if (sportChoices.size() == 0) {
            Toast.makeText(this, "Player has all sports", Toast.LENGTH_SHORT).show();
            return;
        }
        spinner.setAdapter(arrayAdapter);

        builder.setView(dialogView)
                .setTitle(R.string.addSport)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        AlertDialog dialog = (AlertDialog) dialogInterface;

                        EditText ratingInput = dialog.findViewById(R.id.initialRating);

                        try {
                            String sport = sportChoices.get(spinner.getSelectedItemPosition());
                            double rating = Double.parseDouble(ratingInput.getText().toString());
                            if (rating > 0) {
                                player.ratings.put(sport, rating);
                                newSports.add(sport);
                                updateRatings();
                            } else {
                                Toast.makeText(getApplicationContext(), "Rating must be positive", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NullPointerException | NumberFormatException e) {
                            Toast.makeText(getApplicationContext(), "Invalid input", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void submitPlayer() {
        EditText nameEditText = findViewById(R.id.nameEditText);
        String name = nameEditText.getText().toString();

        if (name.length() <= 0) {
            Toast.makeText(getApplicationContext(), "No name given", Toast.LENGTH_SHORT).show();
        } else {
            player.name = name;
            dataViewModel.update(player);
            finish();
        }
    }

    // update the ratings list in the UI
    void updateRatings() {
        final LinearLayout ratingsLayout = findViewById(R.id.ratingsLayout);
        ratingsLayout.removeAllViews();
        ArrayList<String> sports = new ArrayList<>(player.ratings.keySet());
        for (final String sport: sports) {
            final ConstraintLayout row = (ConstraintLayout) getLayoutInflater().inflate(R.layout.ratings_row, null);

            TextView ratingText = row.findViewById(R.id.rating);
            ratingText.setText(String.format("%.2f", player.ratings.get(sport)));

            ImageView iconView = row.findViewById(R.id.icon);
            iconView.setImageResource(Sports.icons.get(sport));

            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            if (newSports.contains(sport)) {
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        player.ratings.remove(sport);
                        newSports.remove(sport);
                        updateRatings();
                    }
                });
            } else {
                deleteButton.setVisibility(View.GONE);
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ratingsLayout.addView(row);
                }
            });
        }
    }
    private void setPlayer(final Player player) {
        this.player = player;

        if (player != null) {
            EditText nameEdit = findViewById(R.id.nameEditText);
            nameEdit.getText().clear();
            nameEdit.append(player.name);

            updateRatings();
        }
    }
}
