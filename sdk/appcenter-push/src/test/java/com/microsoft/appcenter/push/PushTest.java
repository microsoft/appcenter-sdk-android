package com.microsoft.appcenter.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.push.ingestion.models.PushInstallationLog;
import com.microsoft.appcenter.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
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
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings({"unused", "MissingPermission"})
@PrepareForTest({
        Push.class,
        PushInstallationLog.class,
        AppCenterLog.class,
        AppCenter.class,
        StorageHelper.PreferencesStorage.class,
        FirebaseInstanceId.class,
        FirebaseAnalyticsUtils.class,
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
    private AppCenterHandler mAppCenterHandler;

    @Mock
    private AppCenterFuture<Boolean> mBooleanAppCenterFuture;

    @Before
    public void setUp() throws Exception {
        Push.unsetInstance();
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        when(AppCenter.isEnabled()).thenReturn(mBooleanAppCenterFuture);
        when(mBooleanAppCenterFuture.get()).thenReturn(true);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (AppCenter.isEnabled().get()) {
                    ((Runnable) args[0]).run();
                } else if (args[1] instanceof Runnable) {
                    ((Runnable) args[1]).run();
                }
                return null;
            }
        }).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
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
        mockStatic(FirebaseAnalyticsUtils.class);

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
        push.onStarting(mAppCenterHandler);
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
        AppCenterLog.error(anyString(), anyString());

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
        Push.setEnabled(false).thenAccept(new AppCenterConsumer<Void>() {

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
        AppCenterLog.error(anyString(), anyString());

        /* Verify only once to disable Firebase. */
        verifyStatic();
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(false));
        verifyStatic(never());
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(true));

        /* If disabled before start, still we must disable firebase. */
        Push.unsetInstance();
        push = Push.getInstance();
        start(mock(Context.class), push, channel);
        verifyStatic(times(2));
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(false));
        verifyStatic(never());
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(true));
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
    public void verifyEnableFirebaseAnalytics() {
        Context contextMock = mock(Context.class);
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        start(contextMock, push, channel);
        verifyStatic();
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(false));

        /* For check enable firebase analytics collection. */
        Push.enableFirebaseAnalytics(contextMock);
        verifyStatic();
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(true));
    }

    @Test
    public void verifyEnableFirebaseAnalyticsBeforeStart() {
        Context contextMock = mock(Context.class);
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        Push.enableFirebaseAnalytics(contextMock);
        start(contextMock, push, channel);
        verifyStatic(never());
        FirebaseAnalyticsUtils.setEnabled(any(Context.class), eq(false));
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
        RemoteMessage message = mock(RemoteMessage.class);
        RemoteMessage.Notification notification = mock(RemoteMessage.Notification.class);
        when(message.getNotification()).thenReturn(notification);
        when(notification.getTitle()).thenReturn("some title");
        when(notification.getBody()).thenReturn("some message");
        PushMessagingService service = new PushMessagingService();
        service.onMessageReceived(message);
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());
        PushNotification pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertEquals("some title", pushNotification.getTitle());
        assertEquals("some message", pushNotification.getMessage());
        assertEquals(new HashMap<String, String>(), pushNotification.getCustomData());

        /* If disabled, no notification anymore. */
        Push.setEnabled(false);
        service.onMessageReceived(message);

        /* Called once. */
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* Enabled but remove listener. */
        Push.setEnabled(true);
        Push.setListener(null);
        service.onMessageReceived(message);

        /* Called once. */
        verify(pushListener).onPushNotificationReceived(eq(activity), captor.capture());

        /* Mock null notification and custom data. */
        Push.setListener(pushListener);
        Map<String, String> data = new HashMap<>();
        data.put("a", "b");
        data.put("c", "d");
        when(message.getNotification()).thenReturn(null);
        when(message.getData()).thenReturn(data);
        service.onMessageReceived(message);
        verify(pushListener, times(2)).onPushNotificationReceived(eq(activity), captor.capture());
        pushNotification = captor.getValue();
        assertNotNull(pushNotification);
        assertNull(pushNotification.getTitle());
        assertNull(pushNotification.getMessage());
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
        service.onMessageReceived(message);
        Push.setEnabled(false);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* Remove listener while posting to UI thread. */
        Push.setEnabled(true);
        service.onMessageReceived(message);
        Push.setListener(null);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());

        /* Update listener while posting to UI thread. */
        Push.setListener(pushListener);
        service.onMessageReceived(message);
        PushListener pushListener2 = mock(PushListener.class);
        Push.setListener(pushListener2);
        runnable.get().run();
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verify(pushListener2).onPushNotificationReceived(eq(activity), captor.capture());
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
        Intent intent = mock(Intent.class);
        when(activity.getIntent()).thenReturn(intent);
        Bundle extras = mock(Bundle.class);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.getString(Push.EXTRA_GOOGLE_MESSAGE_ID)).thenReturn("reserved value by google");
        final Map<String, String> extraMap = new HashMap<>();
        for (String key : Push.EXTRA_STANDARD_KEYS) {
            extraMap.put(key, "reserved value by google");
        }
        Map<String, String> customMap = new HashMap<>();
        customMap.put("custom", "data");
        customMap.put("b", "c");
        extraMap.putAll(customMap);
        when(extras.keySet()).thenReturn(extraMap.keySet());
        when(extras.getString(anyString())).then(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return extraMap.get(invocation.getArguments()[0].toString());
            }
        });

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
        when(extras.getString(Push.EXTRA_GOOGLE_MESSAGE_ID)).thenReturn("new id");
        Push.setEnabled(false);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verifyStatic(never());
        AppCenterLog.error(anyString(), anyString());

        /* Same effect if we disable App Center. */
        when(mBooleanAppCenterFuture.get()).thenReturn(false);
        push.onActivityResumed(activity);
        verify(pushListener, never()).onPushNotificationReceived(eq(activity), captor.capture());
        verifyStatic(never());
        AppCenterLog.error(anyString(), anyString());

        /* Same if we remove listener. */
        when(mBooleanAppCenterFuture.get()).thenReturn(true);
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
        Intent intent = mock(Intent.class);
        when(activity.getIntent()).thenReturn(intent);
        Bundle extras = mock(Bundle.class);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.getString(Push.EXTRA_GOOGLE_MESSAGE_ID)).thenReturn("some id");
        when(extras.keySet()).thenReturn(Collections.<String>emptySet());

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
        Intent intent = mock(Intent.class);
        Bundle extras = mock(Bundle.class);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.getString(Push.EXTRA_GOOGLE_MESSAGE_ID)).thenReturn("reserved value by google");
        final Map<String, String> extraMap = new HashMap<>();
        for (String key : Push.EXTRA_STANDARD_KEYS) {
            extraMap.put(key, "reserved value by google");
        }
        Map<String, String> customMap = new HashMap<>();
        customMap.put("custom", "data");
        customMap.put("b", "c");
        extraMap.putAll(customMap);
        when(extras.keySet()).thenReturn(extraMap.keySet());
        when(extras.getString(anyString())).then(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return extraMap.get(invocation.getArguments()[0].toString());
            }
        });

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
        AppCenterLog.error(anyString(), anyString());
        Push.checkLaunchedFromNotification(mock(Activity.class), null);
        verifyStatic(times(2));
        AppCenterLog.error(anyString(), anyString());
    }

    @Test
    public void failToInit() {
        IllegalStateException exception = new IllegalStateException();
        when(FirebaseInstanceId.getInstance()).thenThrow(exception);
        Context contextMock = mock(Context.class);
        start(contextMock, Push.getInstance(), mock(Channel.class));
        assertTrue(Push.isEnabled().get());
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), eq(exception));
    }
}