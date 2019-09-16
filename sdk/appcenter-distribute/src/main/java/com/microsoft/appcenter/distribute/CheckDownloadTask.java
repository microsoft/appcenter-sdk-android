/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;

/**
 * Inspect a pending or completed download.
 * This uses APIs that would trigger strict mode exception if used in U.I. thread.
 */
class CheckDownloadTask extends AsyncTask<Void, Void, DownloadProgress> {

    private static final String PACKAGE_INSTALLED_ACTION =
            "com.microsoft.appcenter.distribute.SESSION_API_PACKAGE_INSTALLED";

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
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
        mContext = context.getApplicationContext();
        mDownloadId = downloadId;
        mCheckProgress = checkProgress;
        mReleaseDetails = releaseDetails;
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Uri getFileUriOnOldDevices(Cursor cursor) throws IllegalArgumentException {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }

    @Override
    protected DownloadProgress doInBackground(Void... params) {

        /*
         * Completion might be triggered in background before AppCenter.start
         * if application was killed after starting download.
         *
         * We still want to generate the notification: if we can find the data in preferences
         * that means they were not deleted, and thus that the sdk was not disabled.
         */
        AppCenterLog.debug(LOG_TAG, "Check download id=" + mDownloadId);
        Distribute distribute = Distribute.getInstance();
        if (mReleaseDetails == null) {
            mReleaseDetails = distribute.startFromBackground(mContext);
        }

        /* Check intent data is what we expected. */
        long expectedDownloadId = DistributeUtils.getStoredDownloadId();
        if (expectedDownloadId == INVALID_DOWNLOAD_IDENTIFIER || expectedDownloadId != mDownloadId) {
            AppCenterLog.debug(LOG_TAG, "Ignoring download identifier we didn't expect, id=" + mDownloadId);
            return null;
        }

        /* Query download manager. */
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        try {

            @SuppressWarnings("ConstantConditions")
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
                    if (mCheckProgress) {
                        long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        AppCenterLog.verbose(LOG_TAG, "currentSize=" + currentSize + " totalSize=" + totalSize);
                        return new DownloadProgress(currentSize, totalSize);
                    } else {
                        distribute.markDownloadStillInProgress(mReleaseDetails);
                        return null;
                    }
                }

                /* Build install intent. */
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                AppCenterLog.debug(LOG_TAG, "Download was successful for id=" + mDownloadId + " uri=" + localUri);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    updateThroughIntent(localUri, cursor, distribute);
                } else {
                    updateThroughPackageInstaller(downloadManager.getUriForDownloadedFile(mDownloadId));
                }

            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId, e);
            distribute.completeWorkflow(mReleaseDetails);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private void updateThroughPackageInstaller(Uri localUri) {
        PackageInstaller.Session session = null;
        PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
        try {
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
            int writeResult = writeSession(session, localUri);
            if (writeResult == PackageInstaller.STATUS_FAILURE){
                packageInstaller.abandonSession(sessionId);
                return;
            }

            // Commit the session (this will start the installation workflow).
            session.commit(createStatusReceiver());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't install package", e);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            throw e;
        }
    }

    private IntentSender commitSession(int sessionId) {
        Intent broadcastIntent = new Intent(PACKAGE_INSTALLED_ACTION);
        broadcastIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        broadcastIntent.setPackage("com.microsoft.appcenter.sasquatch.project");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                mContext,
                sessionId,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }

    private IntentSender createStatusReceiver() {
        PackageManager pm = mContext.getPackageManager();
        String packageName = mContext.getPackageName();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.setAction(PACKAGE_INSTALLED_ACTION);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        return pendingIntent.getIntentSender();
    }

    @SuppressLint("NewApi")
    private int writeSession(PackageInstaller.Session session, Uri uri) {
        session.setStagingProgress(0);
        try {
            try (InputStream in = mContext.getContentResolver().openInputStream(uri)) {
                long sizeBytes = 0;
                if (in != null) {
                    sizeBytes = in.available();
                }
                try (OutputStream out = session
                        .openWrite("base.apk", 0, sizeBytes)) {
                    byte[] buffer = new byte[1024 * 1024];
                    while (true) {
                        int numRead = in.read(buffer);
                        if (numRead == -1) {
                            out.flush();
                            session.fsync(out);
                            break;
                        }
                        out.write(buffer, 0, numRead);
                        if (sizeBytes > 0) {
                            float fraction = ((float) numRead / (float) sizeBytes);
                            session.setStagingProgress(fraction);
                        }
                    }
                }
            }
            AppCenterLog.debug(LOG_TAG, "writeSession success");
            return PackageInstaller.STATUS_SUCCESS;
        } catch (IOException | SecurityException e) {
            AppCenterLog.debug(LOG_TAG, String.format("Could not write package %s", e));
            session.close();
            return PackageInstaller.STATUS_FAILURE;
        }
    }

    private void updateThroughIntent(String localUri, Cursor cursor, Distribute distribute) {
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
            AppCenterLog.error(LOG_TAG, "Installer not found");
            distribute.completeWorkflow(mReleaseDetails);
            return;
        }

        /* Check if a should install now. */
        if (!distribute.notifyDownload(mReleaseDetails, intent)) {

            /*
             * This start call triggers strict mode in U.I. thread so it
             * needs to be done here without synchronizing
             * (not to block methods waiting on synchronized on U.I. thread)
             * so yes we could launch install and SDK being disabled...
             *
             * This corner case cannot be avoided without triggering
             * strict mode exception.
             */
            AppCenterLog.info(LOG_TAG, "Show install UI now intentUri=" + intent.getData());
            mContext.startActivity(intent);
            if (mReleaseDetails != null && mReleaseDetails.isMandatoryUpdate()) {
                distribute.setInstalling(mReleaseDetails);
            } else {
                distribute.completeWorkflow(mReleaseDetails);
            }
            storeDownloadedReleaseDetails();
        }
    }

    @Override
    protected void onPostExecute(final DownloadProgress result) {
        if (result != null) {

            /* onPostExecute is not always called on UI thread due to an old Android bug. */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Distribute.getInstance().updateProgressDialog(mReleaseDetails, result);
                }
            });
        }
    }

    /**
     * Store details about downloaded release.
     * After app update and restart, this info is used to report new download and to update group ID (if it's changed).
     */
    private void storeDownloadedReleaseDetails() {
        if (mReleaseDetails == null) {
            AppCenterLog.debug(LOG_TAG, "Downloaded release details are missing or broken, won't store.");
            return;
        }
        String groupId = mReleaseDetails.getDistributionGroupId();
        String releaseHash = mReleaseDetails.getReleaseHash();
        int releaseId = mReleaseDetails.getId();
        AppCenterLog.debug(LOG_TAG, "Store downloaded group id=" + groupId + " release hash=" + releaseHash + " release id=" + releaseId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID, groupId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH, releaseHash);
        SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID, releaseId);
    }
}
