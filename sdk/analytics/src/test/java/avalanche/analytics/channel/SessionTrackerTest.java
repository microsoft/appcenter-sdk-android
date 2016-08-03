package avalanche.analytics.channel;

import android.content.pm.PackageManager;
import android.os.SystemClock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.AbstractLog;
import avalanche.core.ingestion.models.Log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(SystemClock.class)
public class SessionTrackerTest {

    private final static String TEST_GROUP = "group_test";

    @Test
    public void session() throws PackageManager.NameNotFoundException {

        /* Setup mocking. */
        long mockTime = System.currentTimeMillis();
        PowerMockito.mockStatic(SystemClock.class);
        when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        SessionTracker sessionChannel = new SessionTracker(channel, 20);

        /* Application is in background, send a log, verify decoration. */
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Verify session reused for second log. */
        {
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* No usage from background for a long time. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* App comes to foreground and sends a log, still session. */
        {
            sessionChannel.onActivityResumed();
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Switch to another activity. */
        {
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityPaused();
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityResumed();
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a short time and send log: still in session. */
        {
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityPaused();
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a long time and coming back to foreground: new session. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityResumed();
            Log log = new MockLog();
            sessionChannel.onEnqueuingLog(log, TEST_GROUP);
            sessionChannel.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(channel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return "mock";
        }
    }
}
