package com.github.pertempto.backyardsportstracker;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.List;

public class PlayersListFragment extends Fragment {
    private static final String LOG_TAG = "PlayersListFragment";

    private OnPlayerClickListener listener;
    private PlayersRecyclerViewAdapter adapter;
    private DataViewModel dataViewModel;
    private String sport;

    public PlayersListFragment() {}

    @SuppressWarnings("unused")
    public static PlayersListFragment newInstance(int columnCount) {
        PlayersListFragment fragment = new PlayersListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_players_list, container, false);

        Context context = view.getContext();
        final RecyclerView recyclerView = view.findViewById(R.id.list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new PlayersRecyclerViewAdapter(listener);
        recyclerView.setAdapter(adapter);

        dataViewModel = ViewModelProviders.of(getActivity()).get(DataViewModel.class);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "fab clicked");
                Intent intent = new Intent(getActivity(), NewPlayerActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PlayersListFragment.OnPlayerClickListener) {
            listener = (PlayersListFragment.OnPlayerClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPlayerClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.players_list_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSetSport:
                Log.d(LOG_TAG, "set sport");
                setSportDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setSport(SportPreferences.getSelectedSport(getActivity()));
    }

    void setSport(final String sport) {
        Log.d(LOG_TAG, String.format("setting sport: %s", sport));
        this.sport = sport;
        SportPreferences.setSelectedSport(getActivity(), sport);
        final Activity activity = getActivity();
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final List<Player> players = dataViewModel.getAllPlayers();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setPlayers(sport, players);
                    }
                });
            }
        }).execute();
    }

    void setSportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_set_sport, null);

        final Spinner spinner = dialogView.findViewById(R.id.sportChoices);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String sport: Sports.sports) {
            arrayAdapter.add(getString(Sports.names.get(sport)));
        }
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(Sports.sports.indexOf(sport));

        builder.setView(dialogView)
                .setTitle(R.string.setSport)
                .setPositiveButton(R.string.setSport, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        AlertDialog dialog = (AlertDialog) dialogInterface;

                        setSport(Sports.sports.get(spinner.getSelectedItemPosition()));
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    public interface OnPlayerClickListener {
        void onPlayerClick(Player player);
    }
}
