package com.github.pertempto.backyardsportstracker;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.github.pertempto.backyardsportstracker.data.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class PlayerSelectionDialog {
    interface OnPlayersSelectedListener {
        void onPlayersSelected(ArrayList<Player> players);
    }

    private PlayerSelectionDialog() {}

    static void show(Context context, final List<Player> players, final String sport,
                     final OnPlayersSelectedListener listener) {
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player first, Player second) {
                return Double.compare(second.ratings.get(sport), first.ratings.get(sport));
            }
        });

        CharSequence[] labels = new CharSequence[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            labels[i] = String.format(context.getString(R.string.nameAndRatingFormat),
                    player.name, player.ratings.get(sport));
        }

        final boolean[] checkedPlayers = new boolean[players.size()];
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.selectPlayers)
                .setMultiChoiceItems(labels, checkedPlayers,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                checkedPlayers[which] = isChecked;
                                boolean hasSelection = false;
                                for (boolean checked : checkedPlayers) {
                                    hasSelection |= checked;
                                }
                                ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE)
                                        .setEnabled(hasSelection);
                            }
                        })
                .setPositiveButton(R.string.addSelectedPlayers,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ArrayList<Player> selected = new ArrayList<>();
                                for (int i = 0; i < checkedPlayers.length; i++) {
                                    if (checkedPlayers[i]) {
                                        selected.add(players.get(i));
                                    }
                                }
                                if (!selected.isEmpty()) {
                                    listener.onPlayersSelected(selected);
                                }
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ignored) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        dialog.show();
    }
}
