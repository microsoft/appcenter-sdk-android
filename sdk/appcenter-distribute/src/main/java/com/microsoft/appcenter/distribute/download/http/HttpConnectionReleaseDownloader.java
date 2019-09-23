package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.microsoft.appcenter.distribute.InstallerUtils;
import com.microsoft.appcenter.distribute.PermissionsUtil;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    private Listener mListener;
    private static final String PREFERENCE_KEY_DOWNLOADING_FILE = "PREFERENCE_KEY_DOWNLOADING_FILE";
    private DownloadFileTask downloadFileTask;

    public HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails, Listener listener) {
        mListener = listener;
        if (!prepareDownload()){
            return;
        }
        downloadFileTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadFileTask(releaseDetails));
        downloadFileTask.attachListener(mListener);
    }

    @Override
    public void delete() {
        String localFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADING_FILE);
        if(localFilePath == null){
            return;
        }
        mContext.deleteFile(localFilePath);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADING_FILE);
    }

    protected boolean prepareDownload() {
        if (mListener == null){
            AppCenterLog.error(LOG_TAG, "No listener attached, abort downloading.");
            return false;
        }
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            mListener.onError("No network connection, abort downloading.");
            return false;
        }

        String[] permissions = requiredPermissions();
        int[] permissionsState = PermissionsUtil.permissionsState(mContext, permissions);
        if (!PermissionsUtil.permissionsAreGranted(permissionsState)) {
            mListener.onError("No external storage permission.");
            return false;
        }

        if (!InstallerUtils.isUnknownSourcesEnabled(mContext)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                mContext.startActivity(intent);
            } else {
                mListener.onError("Install from unknown sources disabled.");
            }
            return false;
        }

//        startDownloadTask();
//        if (getShowsDialog()) {
//            dismiss();
//        }
        return true;
    }


    private static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    public void removeListener() {
        mListener = null;
        downloadFileTask.detachListener();
    }

    public void cancel(boolean state) {
        downloadFileTask.cancel(state);
    }

}
