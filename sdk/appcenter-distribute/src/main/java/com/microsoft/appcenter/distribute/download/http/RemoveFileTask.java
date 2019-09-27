/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * Removes a downloaded file.
 */
class RemoveFileTask extends AsyncTask<Void, Void, Void> {

    private final File mFile;

    RemoveFileTask(@NonNull File file) {
        mFile = file;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected Void doInBackground(Void... params) {
        mFile.delete();
        return null;
    }
}
