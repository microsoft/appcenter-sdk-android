package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

class ResumeFromBackgroundTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    ResumeFromBackgroundTask(@NonNull Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Distribute distribute = Distribute.getInstance();
        distribute.startFromBackground(mContext);
        distribute.resumeDownload();
        return null;
    }
}
