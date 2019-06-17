/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;

import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultChannelAlternateIngestionTest extends AbstractDefaultChannelTest {

    @Test
    public void nullAppSecretProvided() throws Persistence.PersistenceException {

        /*
         * We don't test empty app secret in channel (and thus in tests) as it's already checked by AppCenter.
         * If app secret is set in channel, the assumption is that it's a well formed string.
         * Channel is not meant to be used publicly directly.
         */

        /* Given a mock channel. */
        Persistence persistence = mock(Persistence.class);
        Ingestion ingestion = mock(Ingestion.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, persistence, ingestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Check log url. */
        String logUrl = "http://mockUrl";
        channel.setLogUrl(logUrl);
        verify(ingestion).setLogUrl(logUrl);

        /* Check enqueue. */
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP, Flags.DEFAULTS);
        verify(persistence, never()).putLog(eq(log), eq(TEST_GROUP), anyInt());
        channel.enqueue(mock(Log.class), "other", Flags.DEFAULTS);
        verify(persistence, never()).putLog(any(Log.class), anyString(), anyInt());

        /* Check clear. Even without app secret it works as it could be logs from previous process. */
        channel.clear(TEST_GROUP);
        verify(persistence).deleteLogs(eq(TEST_GROUP));

        /* Check shutdown. */
        channel.shutdown();
        verify(persistence).clearPendingLogState();
    }

    @Test
    public void useAlternateIngestion() throws IOException {

        /* Set up channel with an alternate ingestion. */
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);
        when(mockPersistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* Enqueuing 1 event. */
        channel.enqueue(mock(Log.class), TEST_GROUP, Flags.DEFAULTS);

        /* Verify that we have called sendAsync on the ingestion. */
        verify(alternateIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(defaultIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getGroupState(TEST_GROUP).mPendingLogCount);

        /* Disabling the channel should close all channels */
        channel.setEnabled(false);
        verify(alternateIngestion).close();
        verify(defaultIngestion).close();

        /* Enabling the channel should reopen all channels */
        channel.setEnabled(true);
        verify(alternateIngestion).reopen();
        verify(defaultIngestion).reopen();
    }

    @Test
    public void startWithoutAppSecret() throws Persistence.PersistenceException {

        /* Set up channel without app secret. */
        String appCenterGroup = "test_group1";
        String oneCollectorGroup = "test_group2";
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);

        /* Simulate we have 1 pending log in storage. */
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));

        /* Create channel and groups. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(appCenterGroup, 2, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.addGroup(oneCollectorGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* App center previous log not sent yet. */
        verify(defaultIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* One collector previous log sent. */
        verify(alternateIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Enqueuing 1 new event for app center. */
        channel.enqueue(mock(Log.class), appCenterGroup, Flags.DEFAULTS);

        /* Not sent. */
        verify(defaultIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify we didn't persist the log since AppCenter not started with app secret. */
        verify(mockPersistence, never()).putLog(any(Log.class), eq(appCenterGroup), eq(Flags.NORMAL));

        /* Enqueuing 1 event from one collector group. */
        channel.enqueue(mock(Log.class), oneCollectorGroup, Flags.DEFAULTS);

        /* Verify that we have called sendAsync on the alternate ingestion a second time. */
        verify(alternateIngestion, times(2)).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(defaultIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we can now send logs to app center after we have set app secret. */
        channel.setAppSecret("testAppSecret");
        channel.enqueue(mock(Log.class), appCenterGroup, Flags.DEFAULTS);
        verify(defaultIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void sendPendingLogsAfterSettingAppSecret() {

        /* Set up channel without app secret. */
        String appCenterGroup = "test_group1";
        String oneCollectorGroup = "test_group2";
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);
        when(mockPersistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));

        /* Simulate we have 1 pending log in storage. */
        when(mockPersistence.countLogs(anyString())).thenReturn(1);

        /* Create channel with the two groups. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(appCenterGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Verify that we can now send logs to app center after we have set app secret. */
        channel.setAppSecret("testAppSecret");
        verify(defaultIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* If we add a one collector group it also resumes. */
        channel.addGroup(oneCollectorGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);
        verify(alternateIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void pendingLogsDisableSetAppSecretThenEnable() {

        /* Set up channel without app secret. */
        String appCenterGroup = "test_group1";
        String oneCollectorGroup = "test_group2";
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);
        when(mockPersistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));

        /* Simulate we have 1 pending log in storage for App Center. */
        when(mockPersistence.countLogs(appCenterGroup)).thenReturn(1);

        /* Create channel with the two groups. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(appCenterGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.addGroup(oneCollectorGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* Disable channel. */
        channel.setEnabled(false);

        /* Verify that no log is sent even if we set app secret while channel is disabled. */
        channel.setAppSecret("testAppSecret");
        verify(defaultIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(alternateIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Enable channel. */
        channel.setEnabled(true);

        /* Verify that now logs are sent when channel is enabled. */
        verify(defaultIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(alternateIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
