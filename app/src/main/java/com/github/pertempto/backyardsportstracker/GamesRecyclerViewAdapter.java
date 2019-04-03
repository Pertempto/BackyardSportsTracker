package com.github.pertempto.backyardsportstracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class GamesRecyclerViewAdapter extends RecyclerView.Adapter<GamesRecyclerViewAdapter.ViewHolder> {

    private final GamesListFragment.OnGameClickListener listener;

    private List<Game> games;

    public GamesRecyclerViewAdapter(GamesListFragment.OnGameClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Game game = games.get(position);
        holder.game = game;
        holder.iconView.setImageResource(Sports.icons.get(game.sport));
        holder.nameView.setText(game.getName(holder.view.getContext()));
        holder.scoreView.setText(String.format("%d - %d", game.team1Score, game.team2Score));

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    // a game has been clicked
                    listener.onGameClick(holder.game);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        if (games != null) {
            return games.size();
        }
        return 0;
    }

    void setGames(List<Game> games) {
        this.games = new ArrayList<>(games);
        Collections.reverse(this.games);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final ImageView iconView;
        final TextView nameView;
        final TextView scoreView;
        Game game;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            iconView = view.findViewById(R.id.icon);
            nameView = view.findViewById(R.id.name);
            scoreView = view.findViewById(R.id.detail);
        }
    }
}
