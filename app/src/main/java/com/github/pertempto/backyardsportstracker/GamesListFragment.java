package com.github.pertempto.backyardsportstracker;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.pertempto.backyardsportstracker.data.BackgroundTask;
import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;

import java.util.List;

public class GamesListFragment extends Fragment {
    private static final String LOG_TAG = "GamesListFragment";

    private OnGameClickListener listener;
    private DataViewModel dataViewModel;

    public GamesListFragment() {}

    @SuppressWarnings("unused")
    public static GamesListFragment newInstance(int columnCount) {
        GamesListFragment fragment = new GamesListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games_list, container, false);

        Context context = view.getContext();
        final RecyclerView recyclerView = view.findViewById(R.id.list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        final GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(listener);
        recyclerView.setAdapter(adapter);

        dataViewModel = ViewModelProviders.of(getActivity()).get(DataViewModel.class);
        final Activity activity = getActivity();
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                final List<Game> games = dataViewModel.getAllGames();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setGames(games);
                    }
                });
            }
        }).execute();

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "fab clicked");
                Intent intent = new Intent(getActivity(), NewGameActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGameClickListener) {
            listener = (OnGameClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGameClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnGameClickListener {
        void onGameClick(Game item);
    }
}
