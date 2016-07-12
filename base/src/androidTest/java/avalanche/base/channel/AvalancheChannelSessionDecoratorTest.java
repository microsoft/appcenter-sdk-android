package avalanche.base.channel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Test;

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

        /* Send a log, verify decoration. */
        Log log = new MockLog();
        sessionChannel.enqueue(log, ANALYTICS_GROUP);
        assertNotNull(log.getSid());
        assertNotNull(log.getDevice());
        verify(channel).enqueue(log, ANALYTICS_GROUP);

        /* Verify session reused for second log. */
        Log log2 = new MockLog();
        sessionChannel.enqueue(log2, ANALYTICS_GROUP);
        assertEquals(log.getSid(), log2.getSid());
        assertEquals(log.getDevice(), log2.getDevice());

        /* Verify new session. */
        Thread.sleep(30);
        packageInfo.versionCode++; // make any change in device properties
        Log log3 = new MockLog();
        sessionChannel.enqueue(log3, ANALYTICS_GROUP);
        assertNotEquals(log.getSid(), log3.getSid());
        assertNotEquals(log.getDevice(), log3.getDevice());

        /* After a long time make believe we just exited an activity and thus still in session. */
        Thread.sleep(30);
        sessionChannel.onActivityPaused(null);
        Log log4 = new MockLog();
        sessionChannel.enqueue(log4, ANALYTICS_GROUP);
        assertEquals(log3.getSid(), log4.getSid());
        assertEquals(log3.getDevice(), log4.getDevice());

        /* Both times are above threshold, new session. */
        Thread.sleep(30);
        packageInfo.versionCode++; // make any change in device properties
        Log log5 = new MockLog();
        sessionChannel.enqueue(log5, ANALYTICS_GROUP);
        assertNotEquals(log4.getSid(), log5.getSid());
        assertNotEquals(log4.getDevice(), log5.getDevice());
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
