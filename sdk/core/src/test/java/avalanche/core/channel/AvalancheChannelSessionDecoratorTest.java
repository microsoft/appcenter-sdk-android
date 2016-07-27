package avalanche.core.channel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import avalanche.core.ingestion.models.AbstractLog;
import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.StartSessionLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(SystemClock.class)
public class AvalancheChannelSessionDecoratorTest {

    /**
     * TODO: Remove this and fix compile errors.
     */
    private final static String ANALYTICS_GROUP = "group_analytics";

    @Test
    public void session() throws PackageManager.NameNotFoundException {

        /* Setup mocking. */
        long mockTime = System.currentTimeMillis();
        PowerMockito.mockStatic(SystemClock.class);
        when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        PackageInfo packageInfo = mock(PackageInfo.class);
        //noinspection WrongConstant
        when(packageManager.getPackageInfo(any(String.class), anyInt())).thenReturn(packageInfo);
        AvalancheChannelSessionDecorator sessionChannel = new AvalancheChannelSessionDecorator(context, channel, 20);

        /* Application is in background, send a log, verify decoration. */
        UUID expectedSid;
        Device expectedDevice;
        StartSessionLog expectedStartSessionLog = new StartSessionLog();
        {
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotNull(log.getSid());
            assertNotNull(log.getDevice());
            verify(channel).enqueue(log, ANALYTICS_GROUP);
            expectedSid = log.getSid();
            expectedDevice = log.getDevice();
            expectedStartSessionLog.setSid(expectedSid);
            expectedStartSessionLog.setDevice(expectedDevice);
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* Verify session reused for second log. */
        {
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* No usage from background for a long time. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            packageInfo.versionCode++; // make any change in device properties
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            assertNotEquals(expectedDevice, log.getDevice());
            expectedSid = log.getSid();
            expectedDevice = log.getDevice();
            expectedStartSessionLog.setSid(expectedSid);
            expectedStartSessionLog.setDevice(expectedDevice);
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* App comes to foreground and sends a log, still session. */
        {
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* Switch to another activity. */
        {
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityPaused(null);
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* Background for a short time and send log: still in session. */
        {
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            sessionChannel.onActivityPaused(null);
            mockTime += 2;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }

        /* Background for a long time and coming back to foreground: new session. */
        {
            mockTime += 30;
            when(SystemClock.elapsedRealtime()).thenReturn(mockTime);
            packageInfo.versionCode++; // make any change in device properties
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            assertNotEquals(expectedDevice, log.getDevice());
            expectedSid = log.getSid();
            expectedDevice = log.getDevice();
            expectedStartSessionLog.setSid(expectedSid);
            expectedStartSessionLog.setDevice(expectedDevice);
            verify(channel).enqueue(expectedStartSessionLog, ANALYTICS_GROUP);
        }
    }

    @Test
    public void packageManagerIsBroken() throws PackageManager.NameNotFoundException {

        /* Setup mocking. */
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        //noinspection WrongConstant
        when(packageManager.getPackageInfo(any(String.class), anyInt())).thenThrow(new PackageManager.NameNotFoundException());
        AvalancheChannel sessionChannel = new AvalancheChannelSessionDecorator(context, channel);
        sessionChannel.enqueue(new MockLog(), ANALYTICS_GROUP);
        verifyZeroInteractions(channel);
    }

    @Test
    public void enableAndDisable() {

        /* Setup mock. */
        AvalancheChannel channel = mock(AvalancheChannel.class);
        AvalancheChannel sessionChannel = new AvalancheChannelSessionDecorator(mock(Context.class), channel);

        /* Check initial state is enabled. */
        when(channel.isEnabled()).thenReturn(true);
        assertTrue(sessionChannel.isEnabled());
        verify(channel).isEnabled();

        /* Check disabling. */
        sessionChannel.setEnabled(false);
        verify(channel).setEnabled(false);
        when(channel.isEnabled()).thenReturn(false);
        assertFalse(sessionChannel.isEnabled());

        /* Check enabling. */
        sessionChannel.setEnabled(true);
        verify(channel).setEnabled(true);
        when(channel.isEnabled()).thenReturn(true);
        assertTrue(sessionChannel.isEnabled());
    }

    private static class MockLog extends AbstractLog {

        @Override
        public String getType() {
            return "mock";
        }
    }
}
