package com.microsoft.appcenter.distribute.download;

import android.os.AsyncTask;

public class DownloadFileTask extends AsyncTask<Void, Long, Long> {

    @Override
    protected Long doInBackground(Void... voids) {
        // TODO download file
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... args) {
        // todo show progress dialog
    }

    @Override
    protected void onPostExecute(Long result) {
        // todo show download result
    }
}
