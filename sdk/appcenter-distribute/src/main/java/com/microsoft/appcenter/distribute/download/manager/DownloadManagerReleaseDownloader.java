package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class DownloadManagerReleaseDownloader implements ReleaseDownloader {
    private Context mContext;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * Current task to check download state and act on it.
     */
    private CheckDownloadTask mCheckDownloadTask;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Distribute service name.
     */
    static final String SERVICE_NAME = "Distribute";

    /**
     * Base key for stored preferences.
     */
    static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

    public DownloadManagerReleaseDownloader(Context context) {
        this.mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails, Listener listener) {
        long downloadId = getStoredDownloadId();
        if (releaseDetails.isMandatoryUpdate() || mCheckedDownload) {
            mCheckDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new CheckDownloadTask(mContext, downloadId, releaseDetails, listener));
        } else {
            mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(mContext, releaseDetails, listener));
            mCheckedDownload = true;
        }
    }

    @Override
    public void delete() {
        long downloadId = getStoredDownloadId();
        if(mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        if(mCheckDownloadTask != null) {
            mCheckDownloadTask.cancel(true);
            mCheckDownloadTask = null;
        }
        if (downloadId >= 0) {
            AppCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
            AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(mContext, downloadId));
        }
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_ID);
    }

    static long getStoredDownloadId() {
        return SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
    }

    synchronized static void removePreviousDownloadId(DownloadManager downloadManager) {
        long previousDownloadId = getStoredDownloadId();
        if (previousDownloadId >= 0) {
            AppCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
            downloadManager.remove(previousDownloadId);
        }
    }
}
