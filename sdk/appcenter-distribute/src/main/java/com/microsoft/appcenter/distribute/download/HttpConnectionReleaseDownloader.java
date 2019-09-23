package com.microsoft.appcenter.distribute.download;

import android.content.Context;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    private Listener mListener;
    private static final String PREFERENCE_KEY_DOWNLOADING_FILE = "PREFERENCE_KEY_DOWNLOADING_FILE";
    private DownloadFileTask downloadFileTask;

    HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails) {
        downloadFileTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadFileTask(mContext, releaseDetails));
        downloadFileTask.attachListener(mListener);
    }

    @Override
    public void delete(long downloadId) {
        String localFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADING_FILE);
        if(localFilePath == null){
            return;
        }
        mContext.deleteFile(localFilePath);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADING_FILE);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void removeListener() {
        mListener = null;
        downloadFileTask.detachListener();
    }

    @Override
    public void cancel(boolean state) {
        downloadFileTask.cancel(state);
    }

}
