package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;

/**
 * Starts distribute service resumes installing downloaded update.
 */
class ResumeFromBackgroundTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    private final long mDownloadedId;

    ResumeFromBackgroundTask(@NonNull Context context, long downloadedId) {
        mContext = context;
        mDownloadedId = downloadedId;
    }

    @Override
    protected Void doInBackground(Void... args) {
        Distribute distribute = Distribute.getInstance();

        /*
         * Completion might be triggered in background before AppCenter.start
         * if application was killed after starting download.
         *
         * We still want to generate the notification: if we can find the data in preferences
         * that means they were not deleted, and thus that the sdk was not disabled.
         */
        distribute.startFromBackground(mContext);

        /* Check download id is what we expected. */
        AppCenterLog.debug(LOG_TAG, "Check download id=" + mDownloadedId);
        long expectedDownloadId = SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
        if (expectedDownloadId == INVALID_DOWNLOAD_IDENTIFIER || expectedDownloadId != mDownloadedId) {
            AppCenterLog.debug(LOG_TAG, "Ignoring download identifier we didn't expect, id=" + mDownloadedId);
            return null;
        }

        /* Resume installing downloaded release. */
        distribute.resumeDownload();
        return null;
    }
}
