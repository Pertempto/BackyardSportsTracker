package com.github.pertempto.backyardsportstracker;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.github.pertempto.backyardsportstracker.data.DataBackupManager;
import com.github.pertempto.backyardsportstracker.data.Database;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class SettingsFragment extends Fragment {
    private static final int REQUEST_EXPORT_BACKUP = 1;
    private static final int REQUEST_IMPORT_BACKUP = 2;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

        Button exportButton = view.findViewById(R.id.exportData);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "backyard-sports-backup.json");
                startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
            }
        });

        Button importButton = view.findViewById(R.id.importData);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
            }
        });

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_BACKUP) {
            exportBackup(uri);
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            confirmImport(uri);
        }
    }

    private void exportBackup(final Uri uri) {
        final Context appContext = getActivity().getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = DataBackupManager.exportJson(Database.getInstance(appContext));
                    OutputStream output = appContext.getContentResolver().openOutputStream(uri);
                    if (output == null) {
                        throw new java.io.IOException("Could not open backup destination.");
                    }
                    try (Writer writer = new OutputStreamWriter(output, "UTF-8")) {
                        writer.write(json);
                    }
                    showBackupResult(appContext, R.string.exportComplete);
                } catch (Exception e) {
                    showBackupResult(appContext, R.string.backupFailed);
                }
            }
        }, "backup-export").start();
    }

    private void confirmImport(final Uri uri) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.importData)
                .setMessage(R.string.importConfirmationMessage)
                .setPositiveButton(R.string.importData, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importBackup(uri);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void importBackup(final Uri uri) {
        final Context appContext = getActivity().getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = appContext.getContentResolver().openInputStream(uri);
                    if (input == null) {
                        throw new java.io.IOException("Could not open backup file.");
                    }
                    try (Reader reader = new InputStreamReader(input, "UTF-8")) {
                        DataBackupManager.importJson(Database.getInstance(appContext), reader);
                    }
                    showBackupResult(appContext, R.string.importComplete);
                } catch (Exception e) {
                    showBackupResult(appContext, R.string.backupFailed);
                }
            }
        }, "backup-import").start();
    }

    private void showBackupResult(final Context context, final int message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
