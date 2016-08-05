package avalanche.analytics.channel;

import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;
import java.util.UUID;

import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.AbstractLog;
import avalanche.core.ingestion.models.Log;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.TimeSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(StorageHelper.PreferencesStorage.class)
public class SessionTrackerTest {

    private final static String TEST_GROUP = "group_test";

    private long mMockTime;
    private TimeSource mTimeSource;
    private AvalancheChannel mChannel;
    private SessionTracker mSessionTracker;

    private void spendTime(long time) {
        mMockTime += time;
        when(mTimeSource.elapsedRealtime()).thenReturn(mMockTime);
        when(mTimeSource.currentTimeMillis()).thenReturn(mMockTime);
    }

    @Before
    public void setUp() {
        mockStatic(StorageHelper.PreferencesStorage.class);
        PowerMockito.doAnswer(new Answer<Void>() {

            @Override
            @SuppressWarnings("unchecked")
            public Void answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                Set<String> value = (Set<String>) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getStringSet(key)).thenReturn(value);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putStringSet(anyString(), anySetOf(String.class));

        mTimeSource = mock(TimeSource.class);
        spendTime(1000);
        mChannel = mock(AvalancheChannel.class);
        mSessionTracker = new SessionTracker(mChannel, 20, mTimeSource);
    }

    @Test
    public void session() throws PackageManager.NameNotFoundException {

        /* Application is in background, send a log, verify decoration. */
        UUID firstSid;
        long firstSessionTime = mMockTime;
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            firstSid = expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Verify session reused for second log. */
        {
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* No usage from background for a long time: new session. */
        {
            spendTime(30);
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* App comes to foreground and sends a log, still session. */
        {
            mSessionTracker.onActivityResumed();
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Switch to another activity. */
        {
            spendTime(2);
            mSessionTracker.onActivityPaused();
            spendTime(2);
            mSessionTracker.onActivityResumed();
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            spendTime(30);
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a short time and send log: still in session. */
        {
            spendTime(2);
            mSessionTracker.onActivityPaused();
            spendTime(2);
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a long time but correlating a log to first session: should not trigger new session. */
        {
            Log log = new MockLog();
            log.setToffset(firstSessionTime + 20);
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(firstSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a long time and coming back to foreground: new session. */
        {
            spendTime(30);
            mSessionTracker.onActivityResumed();
            Log log = new MockLog();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }
    }

    @Test
    public void maxOutStoredSessions() {
        mSessionTracker.onEnqueuingLog(new MockLog(), TEST_GROUP);
        Set<String> sessions = StorageHelper.PreferencesStorage.getStringSet("sessions");
        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        spendTime(30);
        String firstSession = sessions.iterator().next();
        for (int i = 2; i <= 5; i++) {
            mSessionTracker.onEnqueuingLog(new MockLog(), TEST_GROUP);
            Set<String> intermediateSessions = StorageHelper.PreferencesStorage.getStringSet("sessions");
            assertNotNull(intermediateSessions);
            assertEquals(i, intermediateSessions.size());
            spendTime(30);
        }
        mSessionTracker.onEnqueuingLog(new MockLog(), TEST_GROUP);
        Set<String> finalSessions = StorageHelper.PreferencesStorage.getStringSet("sessions");
        assertNotNull(finalSessions);
        assertEquals(5, finalSessions.size());
        assertFalse(finalSessions.contains(firstSession));
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return "mock";
        }
    }
}
