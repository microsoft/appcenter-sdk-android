package com.microsoft.azure.mobile.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.iid.FirebaseInstanceId;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.MobileCenterHandler;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.MobileCenterConsumer;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings({"unused", "MissingPermission"})
@PrepareForTest({
        Push.class,
        PushNotifier.class,
        PushInstallationLog.class,
        PushIntentUtils.class,
        MobileCenterLog.class,
        MobileCenter.class,
        StorageHelper.PreferencesStorage.class,
        FirebaseInstanceId.class,
        HandlerUtils.class
})
public class PushTest {

    private static final String DUMMY_APP_SECRET = "123e4567-e89b-12d3-a456-426655440000";
    private static final String PUSH_ENABLED_KEY = KEY_ENABLED + "_" + Push.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private FirebaseInstanceId mFirebaseInstanceId;

    @Mock
    private MobileCenterHandler mMobileCenterHandler;

    @Mock
    private MobileCenterFuture<Boolean> mBooleanMobileCenterFuture;

    @Before
    public void setUp() throws Exception {
        Push.unsetInstance();
        mockStatic(MobileCenterLog.class);
        mockStatic(MobileCenter.class);
        mockStatic(PushNotifier.class);
        when(MobileCenter.isEnabled()).thenReturn(mBooleanMobileCenterFuture);
        when(mBooleanMobileCenterFuture.get()).thenReturn(true);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (MobileCenter.isEnabled().get()) {
                    ((Runnable) args[0]).run();
                } else if (args[1] instanceof Runnable) {
                    ((Runnable) args[1]).run();
                }
                return null;
            }
        }).when(mMobileCenterHandler).post(any(Runnable.class), any(Runnable.class));

        /* First call to com.microsoft.azure.mobile.MobileCenter.isEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(PUSH_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(PUSH_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(PUSH_ENABLED_KEY), anyBoolean());

        /* Mock Firebase instance. */
        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);

        /* Mock handler. */
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
    }

    private void start(Context contextMock, Push push, Channel channel) {
        push.onStarting(mMobileCenterHandler);
        push.onStarted(contextMock, DUMMY_APP_SECRET, channel);
    }

    @Test
    public void singleton() {
        assertSame(Push.getInstance(), Push.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Push.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(PushInstallationLog.TYPE) instanceof PushInstallationLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void setEnabled() throws InterruptedException {

        /* Before start it's disabled. */
        assertFalse(Push.isEnabled().get());
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());

        /* Start. */
        String testToken = "TEST";
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        when(mFirebaseInstanceId.getToken()).thenReturn(testToken);
        start(mock(Context.class), push, channel);
        verify(channel).removeGroup(eq(push.getGroupName()));
        assertTrue(Push.isEnabled().get());
        verify(mFirebaseInstanceId).getToken();
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* Enable while already enabled. */
        Push.setEnabled(true);
        assertTrue(Push.isEnabled().get());

        /* Verify behavior happened only once. */
        verify(mFirebaseInstanceId).getToken();
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* Disable. */
        Push.setEnabled(false).get();
        assertFalse(Push.isEnabled().get());
        verify(channel).clear(push.getGroupName());
        verify(channel, times(2)).removeGroup(eq(push.getGroupName()));

        /* Disable again. Test waiting with async callback. */
        final CountDownLatch latch = new CountDownLatch(1);
        Push.setEnabled(false).thenAccept(new MobileCenterConsumer<Void>() {

            @Override
            public void accept(Void aVoid) {
                latch.countDown();
            }
        });
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS));

        /* Ignore on token refresh. */
        push.onTokenRefresh(testToken);

        /* Verify behavior happened only once. */
        verify(mFirebaseInstanceId).getToken();
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* Make sure no logging when posting check activity intent commands. */
        Activity activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(mock(Intent.class));
        push.onActivityResumed(activity);

        /* No additional error was logged since before start. */
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());
    }

    @Test
    public void nullTokenOnStartThenRefresh() {

        /* Start. */
        String testToken = "TEST";
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        start(mock(Context.class), push, channel);
        assertTrue(Push.isEnabled().get());
        verify(mFirebaseInstanceId).getToken();
        verify(channel, never()).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* Refresh. */
        push.onTokenRefresh(testToken);
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* Only once. */
        verify(mFirebaseInstanceId).getToken();
    }

    @Test
    public void receivedInForeground() {

        PushListener pushListener = mock(PushListener.class);
        Push.setListener(pushListener);
        Context contextMock = mock(Context.class);
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        start(contextMock, push, channel);
        Activity activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(mock(Intent.class));
        push.onActivityResumed(activity);

        /* Mock some message. */
        Intent pushIntent = createPushIntent("some title", "some message", null);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());
        PushNotification pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertEquals("some title", pushNotification.getTitle());
        assertEquals("some message", pushNotification.getMessage());
        assertEquals(new HashMap<String, String>(), pushNotification.getCustomData());

        /* If disabled, no notification anymore. */
        Push.setEnabled(false);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);

        /* Called once. */
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* Enabled but remove listener. */
        Push.setEnabled(true);
        Push.setListener(null);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);

        /* Called once. */
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* Mock notification and custom data. */
        Push.setListener(pushListener);
        Map<String, String> data = new HashMap<>();
        data.put("a", "b");
        data.put("c", "d");
        pushIntent = createPushIntent("some title", "some message", data);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);
        verify(pushListener, times(2)).onPushNotificationReceived(eq(activity), captor.capture());
        pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertEquals(data, pushNotification.getCustomData());

        /* Disable while posting the command to the U.I. thread. */
        activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(mock(Intent.class));
        push.onActivityResumed(activity);
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                runnable.set((Runnable) invocation.getArguments()[0]);
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        Push.getInstance().onMessageReceived(contextMock, pushIntent);
        Push.setEnabled(false);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* Remove listener while posting to UI thread. */
        Push.setEnabled(true);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);
        Push.setListener(null);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* Update listener while posting to UI thread. */
        Push.setListener(pushListener);
        Push.getInstance().onMessageReceived(contextMock, pushIntent);
        PushListener pushListener2 = mock(PushListener.class);
        Push.setListener(pushListener2);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verify(pushListener2).onPushNotificationReceived(eq(activity), captor.capture());
    }

    @Test
    public void receivedPushInBackgroundWithoutFirebase() {
        IllegalStateException exception = new IllegalStateException();
        when(FirebaseInstanceId.getInstance()).thenThrow(exception);
        Push.getInstance().onMessageReceived(mock(Context.class), mock(Intent.class));
        verifyStatic();
        PushNotifier.handleNotification(any(Context.class), any(Intent.class));
    }

    @Test
    public void receivedPushInBackgroundWithFirebase() {
        Intent intent = createPushIntent(null, null, null);
        Push.getInstance().onMessageReceived(mock(Context.class), intent);
        verifyStatic(never());
        PushNotifier.handleNotification(any(Context.class), any(Intent.class));
    }

    @Test
    public void clickedFromBackground() {
        PushListener pushListener = mock(PushListener.class);
        Push.setListener(pushListener);
        Context contextMock = mock(Context.class);
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        start(contextMock, push, channel);

        /* Mock activity to contain push */
        Activity activity = mock(Activity.class);
        Map<String, String> customMap = new HashMap<>();
        customMap.put("custom", "data");
        customMap.put("b", "c");
        Intent intent = createPushIntent(null, null, customMap);
        when(PushIntentUtils.getGoogleMessageId(intent)).thenReturn("reserved value by google");
        when(activity.getIntent()).thenReturn(intent);

        /* Simulate we detect push in onCreate. */
        push.onActivityCreated(activity, null);
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());
        PushNotification pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertNull(pushNotification.getTitle());
        assertNull(pushNotification.getMessage());
        assertEquals(customMap, pushNotification.getCustomData());

        /* On started on resume will not duplicate the callback. */
        push.onActivityStarted(activity);
        push.onActivityResumed(activity);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* Disable SDK stops callbacks. */
        push.onActivityPaused(activity);
        activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(intent);
        when(PushIntentUtils.getGoogleMessageId(intent)).thenReturn("new id");
        Push.setEnabled(false);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verifyStatic(never());
        MobileCenterLog.error(anyString(), anyString());

        /* Same effect if we disable Mobile Center. */
        when(mBooleanMobileCenterFuture.get()).thenReturn(false);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verifyStatic(never());
        MobileCenterLog.error(anyString(), anyString());

        /* Same if we remove listener. */
        when(mBooleanMobileCenterFuture.get()).thenReturn(true);
        Push.setEnabled(true);
        Push.setListener(null);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* Set listener to read the new push when resumed. */
        Push.setListener(pushListener);
        push.onActivityResumed(activity);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* If intent extras are null, nothing happens. */
        activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(intent);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* If intent contains non push extras, same thing. */
        when(intent.getExtras()).thenReturn(mock(Bundle.class));
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
    }

    @Test
    public void clickedFromBackgroundDisableWhilePostingToUI() {

        /* Mock activity to contain push */
        PushListener pushListener = mock(PushListener.class);
        Push.setListener(pushListener);
        Context contextMock = mock(Context.class);
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        start(contextMock, push, channel);
        Activity activity = mock(Activity.class);
        Intent intent = createPushIntent(null, null, null);
        when(PushIntentUtils.getGoogleMessageId(intent)).thenReturn("some id");
        when(activity.getIntent()).thenReturn(intent);

        /* Disable while posting the command to the U.I. thread. */
        activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(intent);
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                runnable.set((Runnable) invocation.getArguments()[0]);
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        push.onActivityResumed(activity);
        Push.setEnabled(false);
        runnable.get().run();
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
    }

    @Test
    public void clickedFromPausedAndSingleTop() {

        PushListener pushListener = mock(PushListener.class);
        Push.setListener(pushListener);
        Context contextMock = mock(Context.class);
        start(contextMock, Push.getInstance(), mock(Channel.class));

        /* Mock new intent to contain push, but activity with no push in original activity.  */
        Activity activity = mock(Activity.class);
        when(activity.getIntent()).thenReturn(mock(Intent.class));
        Map<String, String> customMap = new HashMap<>();
        customMap.put("custom", "data");
        customMap.put("b", "c");
        Intent intent = createPushIntent(null, null, customMap);
        when(PushIntentUtils.getGoogleMessageId(intent)).thenReturn("some id");

        /* Simulate we detect push in onCreate. */
        Push.checkLaunchedFromNotification(activity, intent);
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());
        PushNotification pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertNull(pushNotification.getTitle());
        assertNull(pushNotification.getMessage());
        assertEquals(customMap, pushNotification.getCustomData());
    }

    @Test
    public void validateCheckLaunchedFromNotification() {
        start(mock(Context.class), Push.getInstance(), mock(Channel.class));
        Push.checkLaunchedFromNotification(null, mock(Intent.class));
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());
        Push.checkLaunchedFromNotification(mock(Activity.class), null);
        verifyStatic(times(2));
        MobileCenterLog.error(anyString(), anyString());
    }

    @Test
    public void registerWithoutFirebase() {
        IllegalStateException exception = new IllegalStateException();
        when(FirebaseInstanceId.getInstance()).thenThrow(exception);
        Push.setSenderId("1234");
        Context contextMock = mock(Context.class);
        start(contextMock, Push.getInstance(), mock(Channel.class));
        assertTrue(Push.isEnabled().get());
        verifyStatic();
        MobileCenterLog.info(anyString(), anyString());
    }

    @Test
    public void registerWithoutFirebaseOrSenderId() {
        IllegalStateException exception = new IllegalStateException();
        when(FirebaseInstanceId.getInstance()).thenThrow(exception);
        Context contextMock = mock(Context.class);
        start(contextMock, Push.getInstance(), mock(Channel.class));
        assertTrue(Push.isEnabled().get());
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());
    }

    @Test
    public void registerWithoutFirebaseStartServiceThrowsIllegalState() {
        when(FirebaseInstanceId.getInstance()).thenThrow(new IllegalStateException());
        Push.setSenderId("1234");
        Context contextMock = mock(Context.class);
        doThrow(new IllegalStateException()).when(contextMock).startService(any(Intent.class));
        start(contextMock, Push.getInstance(), mock(Channel.class));
        assertTrue(Push.isEnabled().get());
        verifyStatic(times(2));
        MobileCenterLog.info(anyString(), anyString());
        Push.getInstance().onActivityResumed(mock(Activity.class));
        verify(contextMock, times(2)).startService(any(Intent.class));
    }

    @Test
    public void registerWithoutFirebaseStartServiceThrowsRuntimeException() {
        when(FirebaseInstanceId.getInstance()).thenThrow(new IllegalStateException());
        Push.setSenderId("1234");
        Context contextMock = mock(Context.class);
        doThrow(new RuntimeException()).when(contextMock).startService(any(Intent.class));
        start(contextMock, Push.getInstance(), mock(Channel.class));
        assertTrue(Push.isEnabled().get());
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString(), any(Exception.class));
    }

    private static Intent createPushIntent(String title, String message, final Map<String, String> customData) {
        mockStatic(PushIntentUtils.class);
        Intent pushIntentMock = mock(Intent.class);
        when(PushIntentUtils.getTitle(pushIntentMock)).thenReturn(title);
        when(PushIntentUtils.getMessage(pushIntentMock)).thenReturn(message);
        if (customData != null) {
            when(PushIntentUtils.getCustomData(pushIntentMock)).thenReturn(customData);
        }
        else {
            when(PushIntentUtils.getCustomData(pushIntentMock)).thenReturn(new HashMap<String, String>());
        }
        return pushIntentMock;
    }
}