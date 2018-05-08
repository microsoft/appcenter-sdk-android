package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener implements Channel.Listener {

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }
}
