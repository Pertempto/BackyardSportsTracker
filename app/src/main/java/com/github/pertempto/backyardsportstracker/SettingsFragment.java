package com.github.pertempto.backyardsportstracker;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.pertempto.backyardsportstracker.data.Database;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button clearButton = view.findViewById(R.id.clearDatabase);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirmation")
                        .setMessage("Are you sure you to clear the app's database?")
                        .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Database.getInstance(getContext()).clearAllTables();
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        return view;
    }
}
