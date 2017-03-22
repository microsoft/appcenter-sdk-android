package com.microsoft.azure.mobile.distribute;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.NoSuchElementException;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.LOG_TAG;

/**
 * Inspect a pending or completed download.
 * This uses APIs that would trigger strict mode exception if used in U.I. thread.
 */
class CheckDownloadTask extends AsyncTask<Void, Void, DownloadProgress> {

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Download identifier to inspect.
     */
    private final long mDownloadId;

    /**
     * Flag to only check progress and not notify or show install U.I. if checking progress while
     * download completed in the meantime.
     */
    private final boolean mCheckProgress;

    /**
     * Release details.
     */
    private ReleaseDetails mReleaseDetails;

    /**
     * Init.
     *
     * @param context        context.
     * @param downloadId     download identifier.
     * @param checkProgress  check progress only.
     * @param releaseDetails release details.
     */
    CheckDownloadTask(Context context, long downloadId, boolean checkProgress, ReleaseDetails releaseDetails) {
        mContext = context;
        mDownloadId = downloadId;
        mCheckProgress = checkProgress;
        mReleaseDetails = releaseDetails;
    }

    @SuppressWarnings("deprecation")
    private static Uri getFileUriOnOldDevices(Cursor cursor) throws IllegalArgumentException {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }

    @Override
    protected DownloadProgress doInBackground(Void... params) {

        /*
         * Completion might be triggered in background before MobileCenter.start
         * if application was killed after starting download.
         *
         * We still want to generate the notification: if we can find the data in preferences
         * that means they were not deleted, and thus that the sdk was not disabled.
         */
        MobileCenterLog.debug(LOG_TAG, "Check download id=" + mDownloadId);
        Distribute distribute = Distribute.getInstance();
        if (!distribute.isStarted()) {
            MobileCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
            StorageHelper.initialize(mContext);
            mReleaseDetails = DistributeUtils.loadCachedReleaseDetails();
        }

        /* Check intent data is what we expected. */
        long expectedDownloadId = DistributeUtils.getStoredDownloadId();
        if (expectedDownloadId == INVALID_DOWNLOAD_IDENTIFIER || expectedDownloadId != mDownloadId) {
            MobileCenterLog.debug(LOG_TAG, "Ignoring download identifier we didn't expect, id=" + mDownloadId);
            return null;
        }

        /* Query download manager. */
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        try {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
            if (cursor == null) {
                throw new NoSuchElementException();
            }
            try {
                if (!cursor.moveToFirst()) {
                    throw new NoSuchElementException();
                }
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_FAILED) {
                    throw new IllegalStateException();
                }
                if (status != DownloadManager.STATUS_SUCCESSFUL || mCheckProgress) {
                    distribute.markDownloadStillInProgress(this);
                    long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    MobileCenterLog.verbose(LOG_TAG, "currentSize=" + currentSize + " totalSize=" + totalSize);
                    return new DownloadProgress(currentSize, totalSize);
                }

                /* Build install intent. */
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                MobileCenterLog.debug(LOG_TAG, "Download was successful for id=" + mDownloadId + " uri=" + localUri);
                Intent intent = DistributeUtils.getInstallIntent(Uri.parse(localUri));
                boolean installerFound = false;
                if (intent.resolveActivity(mContext.getPackageManager()) == null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        intent = DistributeUtils.getInstallIntent(getFileUriOnOldDevices(cursor));
                        installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
                    }
                } else {
                    installerFound = true;
                }
                if (!installerFound) {
                    MobileCenterLog.error(LOG_TAG, "Installer not found");
                    distribute.completeWorkflow(this);
                    return null;
                }

                /* Check if a should install now. */
                if (!distribute.notifyDownload(mContext, this, intent)) {

                    /*
                     * This start call triggers strict mode in U.I. thread so it
                     * needs to be done here without synchronizing
                     * (not to block methods waiting on synchronized on U.I. thread)
                     * so yes we could launch install and SDK being disabled...
                     *
                     * This corner case cannot be avoided without triggering
                     * strict mode exception.
                     */
                    MobileCenterLog.info(LOG_TAG, "Show install UI now.");
                    mContext.startActivity(intent);
                    if (mReleaseDetails != null && mReleaseDetails.isMandatoryUpdate()) {
                        distribute.setInstalling(this);
                    } else {
                        distribute.completeWorkflow(this);
                    }
                }
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            MobileCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId);
            distribute.completeWorkflow(this);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final DownloadProgress result) {
        if (result != null) {

            /* onPostExecute is not always called on UI thread due to an old Android bug. */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Distribute.getInstance().updateProgressDialog(CheckDownloadTask.this, result);
                }
            });
        }
    }

    /**
     * Get context.
     *
     * @return context.
     */
    Context getContext() {
        return mContext;
    }
}
