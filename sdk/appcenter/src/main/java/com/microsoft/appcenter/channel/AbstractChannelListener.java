package com.microsoft.appcenter.channel;


import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * Empty implementation to make callbacks optional.
 */
public class AbstractChannelListener implements Channel.Listener {

    @Override
    public void onGroupAdded(@NonNull String groupName) {
    }

    @Override
    public void onGroupRemoved(@NonNull String groupName) {
    }

    @Override
    public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
    }

    @Override
    public void onPreparedLog(@NonNull Log log, @NonNull String groupName) {
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
}
