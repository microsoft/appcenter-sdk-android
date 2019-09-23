package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.InstallerUtils;
import com.microsoft.appcenter.distribute.PermissionsUtil;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    static final String PREFERENCE_KEY_DOWNLOADING_FILE = "PREFERENCE_KEY_DOWNLOADING_FILE";

    public HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails, @NonNull Listener listener) {
        if (!prepareDownload(listener)){
            return;
        }
        AsyncTaskUtils.execute(LOG_TAG, new DownloadFileTask(releaseDetails, listener));
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

    private boolean prepareDownload(Listener listener) {
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            listener.onError("No network connection, abort downloading.");
            return false;
        }
        String[] permissions = requiredPermissions();
        int[] permissionsState = PermissionsUtil.permissionsState(mContext, permissions);
        if (!PermissionsUtil.permissionsAreGranted(permissionsState)) {
            listener.onError("No external storage permission.");
            return false;
        }
        if (!InstallerUtils.isUnknownSourcesEnabled(mContext)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                mContext.startActivity(intent);
            } else {
                listener.onError("Install from unknown sources disabled.");
            }
            return false;
        }
        return true;
    }

    private static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }
}
