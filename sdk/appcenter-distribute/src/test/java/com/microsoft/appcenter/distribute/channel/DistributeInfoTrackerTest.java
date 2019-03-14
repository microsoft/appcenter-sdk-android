/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.AbstractLog;
import com.microsoft.appcenter.ingestion.models.Log;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unused")
@PrepareForTest(DistributeInfoTracker.class)
public class DistributeInfoTrackerTest {

    private final static String TEST_GROUP = "group_test";

    @NonNull
    private static MockLog newLog() {
        return new MockLog();
    }

    @Test
    public void addDistributionGroupIdToLogsTest() {
        String distributionGroupId = UUID.randomUUID().toString();
        DistributeInfoTracker distributeInfoTracker = new DistributeInfoTracker(distributionGroupId);

        /* Distribution group ID field is added to logs. */
        {
            Log log = newLog();
            distributeInfoTracker.onPreparingLog(log, TEST_GROUP);
            assertNotNull(log.getDistributionGroupId());
            assertEquals(distributionGroupId, log.getDistributionGroupId());
        }
    }

    @Test
    public void setNewDistributionGroupIdTest() {
        DistributeInfoTracker distributeInfoTracker = new DistributeInfoTracker(null);

        /* No distribution group ID field is added to logs because value is null. */
        {
            Log log = newLog();
            distributeInfoTracker.onPreparingLog(log, TEST_GROUP);
            assertNull(log.getDistributionGroupId());
        }

        /* Distribution group ID field is added to logs after the value was fetched and saved. */
        {
            String distributionGroupId = UUID.randomUUID().toString();
            distributeInfoTracker.updateDistributionGroupId(distributionGroupId);
            Log log = newLog();
            distributeInfoTracker.onPreparingLog(log, TEST_GROUP);
            assertEquals(distributionGroupId, log.getDistributionGroupId());
        }

        /* No distribution group ID field is added after the value was removed. */
        {
            distributeInfoTracker.removeDistributionGroupId();
            Log log = newLog();
            distributeInfoTracker.onPreparingLog(log, TEST_GROUP);
            assertNull(log.getDistributionGroupId());
        }
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return null;
        }
    }
}
