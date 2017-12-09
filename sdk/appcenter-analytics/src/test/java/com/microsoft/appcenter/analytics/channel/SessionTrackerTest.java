package com.microsoft.appcenter.analytics.channel;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

@SuppressWarnings("unused")
@PrepareForTest({SessionTracker.class, StorageHelper.PreferencesStorage.class, SystemClock.class})
public class SessionTrackerTest {

    private final static String TEST_GROUP = "group_test";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private long mMockTime;
    private Channel mChannel;
    private SessionTracker mSessionTracker;

    @NonNull
    private static EventLog newEvent() {
        EventLog eventLog = new EventLog();
        eventLog.setId(UUID.randomUUID());
        eventLog.setName("test");
        return eventLog;
    }

    private void spendTime(long time) {
        mMockTime += time;
        when(SystemClock.elapsedRealtime()).thenReturn(mMockTime);
        when(System.currentTimeMillis()).thenReturn(mMockTime);
    }

    @Before
    public void setUp() {
        mockStatic(System.class);
        mockStatic(SystemClock.class);
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
        when(StorageHelper.PreferencesStorage.getStringSet(anyString())).thenReturn(null);
        spendTime(1000);
        mChannel = mock(Channel.class);
        mSessionTracker = new SessionTracker(mChannel, TEST_GROUP);
    }

    @Test
    public void longSessionStartingFromBackground() {

        /* Application is in background, send a log, verify decoration. */
        long firstSessionTime = mMockTime;
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Verify session reused for second log. */
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* No usage from background for a long time: new session. */
        {
            spendTime(30000);
            Log log = newEvent();
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
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            spendTime(30000);
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Switch to another activity and send a log, still session. */
        {
            spendTime(2);
            mSessionTracker.onActivityPaused();
            spendTime(2);
            mSessionTracker.onActivityResumed();
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            spendTime(30000);
            Log log = newEvent();
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
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a long time and coming back to foreground: new session. */
        {
            spendTime(30000);
            mSessionTracker.onActivityResumed();
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Background for a long time sending a log: new session. */
        {
            mSessionTracker.onActivityPaused();
            spendTime(30000);
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }
    }

    @Test
    public void stayOnFirstScreenForLong() {

        /* Application is in foreground, send a log, verify decoration with a new session. */
        mSessionTracker.onActivityResumed();
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Wait a long time. */
        spendTime(30000);

        /* Go to another activity. */
        mSessionTracker.onActivityPaused();
        spendTime(2);
        mSessionTracker.onActivityResumed();

        /* Send a log again: session must be reused. */
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }
    }

    @Test
    public void goBackgroundAndComeBackMuchLater() {

        /* Application is in foreground, send a log, verify decoration with a new session. */
        mSessionTracker.onActivityResumed();
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Go background. */
        spendTime(1);
        mSessionTracker.onActivityPaused();

        /* Come back after a long time. */
        spendTime(30000);
        mSessionTracker.onActivityResumed();

        /* Send a log again: new session. */
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* In total we sent only 2 session logs. */
        verify(mChannel, times(2)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof StartSessionLog;
            }
        }), anyString());
    }

    @Test
    public void startSessionWithoutLogs() {

        final AtomicReference<StartSessionLog> startSessionLog = new AtomicReference<>();
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                startSessionLog.set((StartSessionLog) invocation.getArguments()[0]);
                return null;
            }
        }).when(mChannel).enqueue(notNull(StartSessionLog.class), eq(TEST_GROUP));

        /* Go foreground, start session is sent. */
        mSessionTracker.onActivityResumed();
        verify(mChannel, times(1)).enqueue(notNull(StartSessionLog.class), eq(TEST_GROUP));
        assertNotNull(startSessionLog.get());
        UUID sid = startSessionLog.get().getSid();
        assertNotNull(sid);

        /* Change screen after a long time, session reused. */
        spendTime(30000);
        mSessionTracker.onActivityPaused();
        spendTime(1);
        mSessionTracker.onActivityResumed();
        verify(mChannel, times(1)).enqueue(notNull(StartSessionLog.class), eq(TEST_GROUP));

        /* Go background and come back after timeout, second session. */
        spendTime(1);
        mSessionTracker.onActivityPaused();
        spendTime(30000);
        mSessionTracker.onActivityResumed();
        verify(mChannel, times(2)).enqueue(notNull(StartSessionLog.class), eq(TEST_GROUP));
        assertNotEquals(sid, startSessionLog.get().getSid());
    }

    @Test
    public void sdkConfiguredBetweenPauseAndResume() {

        /* Pause application before we saw the first resume event (integration problem). We are handling that gracefully though. */
        mSessionTracker.onActivityPaused();

        /* Application is in background, send a log, verify decoration. */
        UUID expectedSid;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotNull(log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* Verify session reused for second log. */
        {
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* No usage from background for a long time: new session. */
        {
            spendTime(30000);
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            expectedSid = log.getSid();
            expectedStartSessionLog.setSid(expectedSid);
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* App comes to foreground and sends a log, we were in background for a long time but we sent a log recently, still session. */
        {
            mSessionTracker.onActivityResumed();
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            spendTime(30000);
            Log log = newEvent();
            mSessionTracker.onEnqueuingLog(log, TEST_GROUP);
            mSessionTracker.onEnqueuingLog(expectedStartSessionLog, TEST_GROUP);
            assertEquals(expectedSid, log.getSid());
            verify(mChannel).enqueue(expectedStartSessionLog, TEST_GROUP);
        }
    }

    @Test
    public void ignoreStartService() {
        Log startServiceLog = spy(new StartServiceLog());
        mSessionTracker.onEnqueuingLog(startServiceLog, TEST_GROUP);
        verify(mChannel, never()).enqueue(any(Log.class), anyString());
        verify(startServiceLog, never()).setSid(any(UUID.class));
    }
}
