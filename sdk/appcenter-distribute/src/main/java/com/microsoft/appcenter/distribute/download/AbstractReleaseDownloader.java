/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import androidx.annotation.NonNull;

import com.microsoft.appcenter.distribute.ReleaseDetails;

public abstract class AbstractReleaseDownloader implements ReleaseDownloader {

    /**
     * Context.
     */
    protected final Context mContext;

    /**
     * Release to download.
     */
    protected final ReleaseDetails mReleaseDetails;

    /**
     * Listener of download status.
     */
    protected final ReleaseDownloader.Listener mListener;

    private boolean mCancelled;

    protected AbstractReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull ReleaseDownloader.Listener listener) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mListener = listener;
    }

    protected boolean isCancelled() {
        return mCancelled;
    }

    @NonNull
    @Override
    public ReleaseDetails getReleaseDetails() {
        return mReleaseDetails;
    }

    @Override
    public void cancel() {
        mCancelled = true;
    }
}
