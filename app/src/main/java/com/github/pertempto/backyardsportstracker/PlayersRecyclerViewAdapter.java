package com.github.pertempto.backyardsportstracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Player;
import com.github.pertempto.backyardsportstracker.data.PlayerStats;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PlayersRecyclerViewAdapter extends RecyclerView.Adapter<PlayersRecyclerViewAdapter.ViewHolder> {

    private final PlayersListFragment.OnPlayerClickListener listener;

    private String sport;
    private List<Player> players;
    private final HashMap<Long, PlayerStats> records = new HashMap<>();

    public PlayersRecyclerViewAdapter(PlayersListFragment.OnPlayerClickListener listener) {
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
        Player player = players.get(position);
        holder.player = player;
        holder.iconView.setImageResource(Sports.icons.get(sport));
        holder.nameView.setText(player.name);
        PlayerStats stats = records.get(player.id);
        holder.recordView.setText(holder.view.getContext().getString(
                stats.ties == 0 ? R.string.winLossFormat : R.string.winLossTieFormat,
                stats.wins, stats.losses, stats.ties));

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    // a player has been clicked
                    listener.onPlayerClick(holder.player);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        if (players != null) {
            return players.size();
        }
        return 0;
    }

    void setPlayers(final String sport, List<Player> players, List<Game> games) {
        this.sport = sport;
        this.players = new ArrayList<>();
        records.clear();
        for (Player player: players) {
            if (player.ratings.containsKey(sport)) {
                this.players.add(player);
                records.put(player.id, PlayerStats.fromGames(player, games, sport));
            }
        }
        PlayerStats.sortPlayers(this.players, games, sport);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final ImageView iconView;
        final TextView nameView;
        final TextView recordView;
        Player player;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            iconView = view.findViewById(R.id.icon);
            nameView = view.findViewById(R.id.name);
            recordView = view.findViewById(R.id.detail);
        }
    }
}
