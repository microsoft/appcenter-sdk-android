package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        channel.enqueue(log, TEST_GROUP);
        verify(persistence, never()).putLog(TEST_GROUP, log);
        channel.enqueue(mock(Log.class), "other");
        verify(persistence, never()).putLog(anyString(), any(Log.class));

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
        when(mockPersistence.getLogs(any(String.class), anyInt(), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* Enqueuing 1 event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify that we have called sendAsync on the ingestion. */
        verify(alternateIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(defaultIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

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
        when(mockPersistence.getLogs(any(String.class), anyInt(), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(appCenterGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.addGroup(oneCollectorGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* Enqueuing 1 event. */
        channel.enqueue(mock(Log.class), appCenterGroup);

        /* Verify that we have called sendAsync on the ingestion. */
        verify(alternateIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(defaultIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* The counter should be 0 because we did not provide app secret. */
        assertEquals(0, channel.getCounter(appCenterGroup));

        /* Verify we didn't persist the log. */
        verify(mockPersistence, never()).putLog(eq(appCenterGroup), any(Log.class));

        /* Enqueuing 1 event from one collector group. */
        channel.enqueue(mock(Log.class), oneCollectorGroup);

        /* Verify that we have called sendAsync on the ingestion. */
        verify(alternateIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(defaultIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we can now send logs to app center after we have set app secret. */
        channel.setAppSecret("testAppSecret");
        channel.enqueue(mock(Log.class), appCenterGroup);
        verify(defaultIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void sendPendingLogsAfterSettingAppSecret() {

        /* Set up channel without app secret. */
        String appCenterGroup = "test_group1";
        String oneCollectorGroup = "test_group2";
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);
        when(mockPersistence.getLogs(any(String.class), anyInt(), anyListOf(Log.class))).then(getGetLogsAnswer(1));

        /* Simulate we have 1 pending log in storage. */
        when(mockPersistence.countLogs(anyString())).thenReturn(1);

        /* Create channel with the two groups. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mockPersistence, defaultIngestion, mAppCenterHandler);
        channel.addGroup(appCenterGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.addGroup(oneCollectorGroup, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, alternateIngestion, null);

        /* Verify that we can now send logs to app center after we have set app secret. */
        channel.setAppSecret("testAppSecret");
        verify(defaultIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(alternateIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void pendingLogsDisableSetAppSecretThenEnable() {

        /* Set up channel without app secret. */
        String appCenterGroup = "test_group1";
        String oneCollectorGroup = "test_group2";
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion defaultIngestion = mock(Ingestion.class);
        Ingestion alternateIngestion = mock(Ingestion.class);
        when(mockPersistence.getLogs(any(String.class), anyInt(), anyListOf(Log.class))).then(getGetLogsAnswer(1));

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
        verify(defaultIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(alternateIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Enable channel. */
        channel.setEnabled(true);

        /* Verify that now logs are sent when channel is enabled. */
        verify(defaultIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(alternateIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
