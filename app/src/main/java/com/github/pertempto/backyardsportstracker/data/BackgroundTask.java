package com.github.pertempto.backyardsportstracker.data;

import android.os.AsyncTask;

public class BackgroundTask extends AsyncTask<Void, Void, Void> {
    BackgroundTaskCallback callback;

    public BackgroundTask(BackgroundTaskCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        callback.call();
        return null;
    }

    public interface BackgroundTaskCallback {
        void call();
    }
}
