package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

class DownloadManagerReleaseDownloader implements ReleaseDownloader {
    private Context mContext;
    private Listener mListener;

    /**
     * Current task to check download state and act on it.
     */
    private CheckDownloadTask mCheckDownloadTask;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * Package info.
     */
    private PackageInfo mPackageInfo;


    DownloadManagerReleaseDownloader(Context context) {
        this.mContext = context;
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            AppCenterLog.error(LOG_TAG, "Could not get self package info.", e);
        }
    }

    @Override
    public void download(ReleaseDetails releaseDetails) {
        mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(mContext, releaseDetails));
    }

    /*private void startCheckDownloadTask(ReleaseDetails releaseDetails) {
        boolean checkProgress = true;

        *//* Querying download manager and even the start intent are detected by strict mode so we do that in background. *//*
        mCheckDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new CheckDownloadTask(mContext, downloadId, checkProgress, releaseDetails));
        mCheckDownloadTask.attachListener(mListener);
    }*/

    @Override
    public void delete(long downloadId) {
        AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(mContext, downloadId));
    }

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener() {
        mListener = null;
        mCheckDownloadTask.detachListener();
    }

    @Override
    public void cancel(boolean state) {
        this.mDownloadTask.cancel(state);
    }
}
