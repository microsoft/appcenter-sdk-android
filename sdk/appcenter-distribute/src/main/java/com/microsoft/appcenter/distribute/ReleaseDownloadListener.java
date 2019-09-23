/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.distribute.download.ReleaseDownloader;

public class ReleaseDownloadListener implements ReleaseDownloader.Listener {
    @Override
    public void onProgress(long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onComplete(String localUri) {

    }

    @Override
    public void onError(String errorMessage) {

    }
}
