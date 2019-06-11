/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;


import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * Empty implementation to make callbacks optional.
 */
public class AbstractChannelListener implements Channel.Listener {

    @Override
    public void onGroupAdded(@NonNull String groupName, Channel.GroupListener groupListener, long batchTimeInterval) {
    }

    @Override
    public void onGroupRemoved(@NonNull String groupName) {
    }

    @Override
    public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
    }

    @Override
    public void onPreparedLog(@NonNull Log log, @NonNull String groupName, int flags) {
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }

    @Override
    public void onGloballyEnabled(boolean isEnabled) {
    }

    @Override
    public void onClear(@NonNull String groupName) {
    }

    @Override
    public void onPaused(@NonNull String groupName, String targetToken) {
    }

    @Override
    public void onResumed(@NonNull String groupName, String targetToken) {
    }
}
