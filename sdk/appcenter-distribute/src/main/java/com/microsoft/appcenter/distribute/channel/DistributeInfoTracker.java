/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.ingestion.models.Log;

/**
 * Channel listener which adds extra fields to logs.
 */
public class DistributeInfoTracker extends AbstractChannelListener {

    /**
     * Distribution group ID that is added to logs (if exists).
     */
    private String mDistributionGroupId;

    /**
     * Init.
     *
     * @param distributionGroupId distribution group ID that is added to logs (if exists).
     */
    public DistributeInfoTracker(String distributionGroupId) {
        mDistributionGroupId = distributionGroupId;
    }

    @Override
    synchronized public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
        if (mDistributionGroupId == null) {
            return;
        }

        /* Set current distribution group ID. */
        log.setDistributionGroupId(mDistributionGroupId);
    }

    /**
     * Update the distribution group ID value that is added to logs.
     *
     * @param distributionGroupId the distribution group ID value that is added to logs.
     */
    synchronized public void updateDistributionGroupId(String distributionGroupId) {
        mDistributionGroupId = distributionGroupId;
    }

    /**
     * Don't add the distribution group ID value to logs.
     */
    synchronized public void removeDistributionGroupId() {
        mDistributionGroupId = null;
    }
}
