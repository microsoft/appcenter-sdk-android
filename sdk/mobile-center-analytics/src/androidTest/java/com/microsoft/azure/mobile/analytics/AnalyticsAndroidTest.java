package com.microsoft.azure.mobile.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.analytics.channel.AnalyticsListener;
import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.test.TestUtils.TAG;
import static com.microsoft.azure.mobile.utils.UUIDUtils.randomUUID;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AnalyticsAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        MobileCenterLog.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getContext();
        Constants.loadFromContext(sContext);
        StorageHelper.initialize(sContext);
    }

    @Before
    public void cleanup() {
        android.util.Log.i(TAG, "Cleanup");
        Analytics.unsetInstance();
        StorageHelper.PreferencesStorage.clear();
    }

    @Test
    public void testAnalyticsListener() {

        AnalyticsListener analyticsListener = mock(AnalyticsListener.class);
        Analytics.setListener(analyticsListener);
        Channel channel = mock(Channel.class);
        Analytics.getInstance().onStarted(sContext, "", channel);
        Analytics.trackEvent("event");

        /* First process: enqueue log but network is down... */
        final EventLog log = new EventLog();
        log.setId(randomUUID());
        log.setName("name");
        Analytics.unsetInstance();
        Analytics.setListener(analyticsListener);
        verify(channel).enqueue(any(Log.class), anyString());
        verifyNoMoreInteractions(analyticsListener);

        /* Second process: sending succeeds. */
        final AtomicReference<Channel.GroupListener> groupListener = new AtomicReference<>();
        channel = mock(Channel.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Channel.GroupListener listener = (Channel.GroupListener) invocationOnMock.getArguments()[4];
                groupListener.set(listener);
                listener.onBeforeSending(log);
                return null;
            }
        }).when(channel).addGroup(anyString(), anyInt(), anyInt(), anyInt(), any(Channel.GroupListener.class));
        Analytics.unsetInstance();
        Analytics.setListener(analyticsListener);
        Analytics.getInstance().onStarted(sContext, "", channel);
        assertNotNull(groupListener.get());
        groupListener.get().onSuccess(log);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        verify(analyticsListener).onBeforeSending(any(EventLog.class));
        verify(analyticsListener).onSendingSucceeded(any(EventLog.class));
        verifyNoMoreInteractions(analyticsListener);
    }
}
