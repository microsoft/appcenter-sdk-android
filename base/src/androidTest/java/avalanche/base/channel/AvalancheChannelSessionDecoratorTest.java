package avalanche.base.channel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Test;

import java.util.UUID;

import avalanche.base.ingestion.models.Device;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.json.MockLog;

import static avalanche.base.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AvalancheChannelSessionDecoratorTest {

    @Test
    public void session() throws PackageManager.NameNotFoundException, InterruptedException {

        /* Setup mocking. */
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        PackageInfo packageInfo = mock(PackageInfo.class);
        //noinspection WrongConstant
        when(packageManager.getPackageInfo(any(String.class), anyInt())).thenReturn(packageInfo);
        AvalancheChannelSessionDecorator sessionChannel = new AvalancheChannelSessionDecorator(context, channel);
        sessionChannel.setSessionTimeout(20);

        /* Application is in background, send a log, verify decoration. */
        UUID expectedSid;
        Device expectedDevice;
        {
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotNull(log.getSid());
            assertNotNull(log.getDevice());
            verify(channel).enqueue(log, ANALYTICS_GROUP);
            expectedSid = log.getSid();
            expectedDevice = log.getDevice();
        }

        /* Verify session reused for second log. */
        {
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
        }

        /* No usage from background for a long time. */
        {
            Thread.sleep(30);
            packageInfo.versionCode++; // make any change in device properties
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            assertNotEquals(expectedDevice, log.getDevice());
            expectedSid = log.getSid();
            expectedDevice = log.getDevice();
        }

        /* App comes to foreground and sends a log, still session. */
        {
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
        }

        /* Switch to another activity. */
        {
            Thread.sleep(2);
            sessionChannel.onActivityPaused(null);
            Thread.sleep(2);
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
        }

        /* We are in foreground, even after timeout a log is still in session. */
        {
            Thread.sleep(30);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
        }

        /* Background for a short time and send log: still in session. */
        {
            Thread.sleep(2);
            sessionChannel.onActivityPaused(null);
            Thread.sleep(2);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertEquals(expectedSid, log.getSid());
            assertEquals(expectedDevice, log.getDevice());
        }

        /* Background for a long time and coming back to foreground: new session. */
        {
            Thread.sleep(30);
            packageInfo.versionCode++; // make any change in device properties
            sessionChannel.onActivityResumed(null);
            Log log = new MockLog();
            sessionChannel.enqueue(log, ANALYTICS_GROUP);
            assertNotEquals(expectedSid, log.getSid());
            assertNotEquals(expectedDevice, log.getDevice());
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
}
