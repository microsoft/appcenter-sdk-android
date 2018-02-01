package com.microsoft.appcenter.distribute.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.AbstractLog;
import com.microsoft.appcenter.ingestion.models.Log;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.UUID;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;


@SuppressWarnings("unused")
@PrepareForTest(DistributeInfoTracker.class)
public class DistributeInfoTrackerTest {

    private final static String TEST_GROUP = "group_test";

    private Channel mChannel;

    @NonNull
    private static MockLog newLog() {
        MockLog log = new MockLog();
        return log;
    }

    @Before
    public void setUp() {
        mChannel = mock(Channel.class);
    }

    @Test
    public void addDistributionGroupIdToLogsTest() {
        String distributionGroupId = UUID.randomUUID().toString();
        DistributeInfoTracker distributeInfoTracker = new DistributeInfoTracker(distributionGroupId);

        /* Distribution group ID field is added to logs. */
        {
            Log log = newLog();
            distributeInfoTracker.onEnqueuingLog(log, TEST_GROUP);
            assertNotNull(log.getDistributionGroupId());
            assertEquals(distributionGroupId, log.getDistributionGroupId());
        }
    }

    @Test
    public void setNewDistributionGroupIdTest() {
        String distributionGroupId1 = null;
        String distributionGroupId2 = UUID.randomUUID().toString();
        DistributeInfoTracker distributeInfoTracker = new DistributeInfoTracker(distributionGroupId1);

        /* No distribution group ID field is added to logs because value is null. */
        {
            Log log = newLog();
            distributeInfoTracker.onEnqueuingLog(log, TEST_GROUP);
            assertNull(log.getDistributionGroupId());
        }

        /* Distribution group ID field is added to logs after the value was fetched and saved. */
        {
            distributeInfoTracker.updateDistributionGroupId(distributionGroupId2);
            Log log = newLog();
            distributeInfoTracker.onEnqueuingLog(log, TEST_GROUP);
            assertEquals(distributionGroupId2, log.getDistributionGroupId());
        }

        /* No distribution group ID field is added after the value was removed. */
        {
            distributeInfoTracker.removeDistributionGroupId();
            Log log = newLog();
            distributeInfoTracker.onEnqueuingLog(log, TEST_GROUP);
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
