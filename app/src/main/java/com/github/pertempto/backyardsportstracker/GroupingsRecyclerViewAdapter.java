package com.github.pertempto.backyardsportstracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Sports;

import java.util.List;


public class GroupingsRecyclerViewAdapter extends RecyclerView.Adapter<GroupingsRecyclerViewAdapter.ViewHolder> {

    private final GroupingsListFragment.OnGroupingClickListener listener;

    private List<Grouping> groupings;

    public GroupingsRecyclerViewAdapter(GroupingsListFragment.OnGroupingClickListener listener) {
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
        final Grouping grouping = groupings.get(position);
        holder.grouping = grouping;
        holder.iconView.setImageResource(Sports.icons.get(grouping.sport));
        holder.nameView.setText(grouping.getName(holder.view.getContext()));

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    // a grouping has been clicked
                    listener.onGroupingClick(holder.grouping);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        if (groupings != null) {
            return groupings.size();
        }
        return 0;
    }

    void setGroupings(List<Grouping> groupings) {
        this.groupings = groupings;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final ImageView iconView;
        final TextView nameView;
        Grouping grouping;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            iconView = view.findViewById(R.id.icon);
            nameView = view.findViewById(R.id.name);
        }
    }
}
