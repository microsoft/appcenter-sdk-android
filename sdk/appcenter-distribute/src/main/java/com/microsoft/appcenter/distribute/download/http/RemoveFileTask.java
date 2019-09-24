/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

/**
 * Removes a downloaded file.
 */
class RemoveFileTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    private final String mFilePath;

    RemoveFileTask(@NonNull Context context, @NonNull String filePath) {
        mContext = context;
        mFilePath = filePath;
    }

    @Override
    protected Void doInBackground(Void... params) {
        mContext.deleteFile(mFilePath);
        return null;
    }
}
