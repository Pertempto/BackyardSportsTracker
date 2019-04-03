package com.github.pertempto.backyardsportstracker;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NewPlayerActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NewPlayerActivity";

    private DataViewModel dataViewModel;

    private HashMap<String, Double> initialRatings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_player);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        initialRatings = new HashMap<>();
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
                submitPlayer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onAddSportClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_sport_rating, null);

        final Spinner spinner = dialogView.findViewById(R.id.sportChoices);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        arrayAdapter.add(getString(R.string.all));
        for (String sport: Sports.sports) {
            arrayAdapter.add(getString(Sports.names.get(sport)));
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
                            String sport = Sports.ALL;
                            if (spinner.getSelectedItemPosition() != 0) {
                                sport = Sports.sports.get(spinner.getSelectedItemPosition() - 1);
                            }
                            double rating = Double.parseDouble(ratingInput.getText().toString());
                            if (rating > 0) {
                                if (sport == Sports.ALL) {
                                    for (String s: Sports.sports) {
                                        initialRatings.put(s, rating);
                                    }
                                } else {
                                    initialRatings.put(sport, rating);
                                }
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

    void submitPlayer() {
        EditText nameEditText = findViewById(R.id.nameEditText);
        String name = nameEditText.getText().toString();

        if (name.length() <= 0) {
            Toast.makeText(getApplicationContext(), "No name given", Toast.LENGTH_SHORT).show();
        } else if (initialRatings.size() == 0){
            Toast.makeText(getApplicationContext(), "Must have at least one sport", Toast.LENGTH_SHORT).show();
        } else {
            dataViewModel.insert(new Player(name, initialRatings));
            finish();
        }
    }

    // update the ratings list in the UI
    void updateRatings() {
        ArrayList<String> sports = new ArrayList<>(initialRatings.keySet());
        Collections.sort(sports);

        LinearLayout ratingsLayout = findViewById(R.id.ratingsLayout);
        ratingsLayout.removeAllViews();

        for (final String sport : sports) {
            View row = getLayoutInflater().inflate(R.layout.ratings_row, null);

            TextView ratingText = row.findViewById(R.id.rating);
            ratingText.setText(initialRatings.get(sport).toString());

            ImageView iconView = row.findViewById(R.id.icon);
            iconView.setImageResource(Sports.icons.get(sport));

            ImageButton deleteButton = row.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialRatings.remove(sport);
                    updateRatings();
                }
            });
            ratingsLayout.addView(row);
        }
    }
}
