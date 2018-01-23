package com.microsoft.appcenter.channel;


import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * Empty implementation to make callbacks optional.
 */
public class AbstractChannelListener implements Channel.Listener {

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }
}
