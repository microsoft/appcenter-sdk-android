/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.download.http.HttpConnectionReleaseDownloader.PREFERENCE_KEY_DOWNLOADED_FILE;

/**
 * Removes a downloaded apk.
 */
public class RemoveHttpDownloadTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    /**
     * Init.
     *
     * @param context    context.
     */
    RemoveHttpDownloadTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String localFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_FILE);
        if(localFilePath == null){
            return null;
        }
        mContext.deleteFile(localFilePath);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_FILE);
        return null;
    }
}
