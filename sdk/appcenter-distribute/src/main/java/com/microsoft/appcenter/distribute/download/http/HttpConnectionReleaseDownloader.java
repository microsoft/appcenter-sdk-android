package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.InstallerUtils;
import com.microsoft.appcenter.distribute.PermissionsUtil;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    static final String PREFERENCE_KEY_DOWNLOADED_FILE = "PREFERENCE_KEY_DOWNLOADED_FILE";

    public HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails, @NonNull Listener listener) {
        if (!prepareDownload(listener)) {
            return;
        }
        AsyncTaskUtils.execute(LOG_TAG, new DownloadFileTask(releaseDetails, listener));
    }

    @Override
    public void delete() {
        AsyncTaskUtils.execute(LOG_TAG, new RemoveHttpDownloadTask(mContext));
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
            listener.onError("Install from unknown sources disabled.");
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
