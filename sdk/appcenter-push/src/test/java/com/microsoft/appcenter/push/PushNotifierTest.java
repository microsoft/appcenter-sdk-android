/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AppNameHelper;

import org.junit.After;
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

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.microsoft.appcenter.push.PushNotifier.DEFAULT_COLOR_METADATA_NAME;
import static com.microsoft.appcenter.push.PushNotifier.DEFAULT_ICON_METADATA_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings({"unused", "deprecation"})
@PrepareForTest({
        PushIntentUtils.class,
        PushNotifier.class,
        AppNameHelper.class,
        Color.class,
        TextUtils.class
})
public class PushNotifierTest {

    private final String mDummyGoogleMessageId = "messageId";

    private final int mIconId = 29;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContextMock;

    @Mock
    private NotificationManager mNotificationManagerMock;

    @Mock
    private Notification mNotificationMock;

    @Mock
    private Notification.Builder mNotificationBuilderMock;

    @Mock
    private Intent mActionIntentMock;

    @Mock
    private PackageManager mPackageManagerMock;

    private ApplicationInfo mApplicationInfoMock;

    private static void setVersionSdkInt(int versionSdkInt) throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", versionSdkInt);
    }

    @Before
    public void setUp() throws Exception {
        setVersionSdkInt(Build.VERSION_CODES.O);
        mockStatic(PushIntentUtils.class);
        when(PushIntentUtils.getMessage(any(Intent.class))).thenReturn("message");
        when(PushIntentUtils.getMessageId(any(Intent.class))).thenReturn(mDummyGoogleMessageId);
        when(PushIntentUtils.getCustomData(any(Intent.class))).thenReturn(new HashMap<String, String>());
        when(mNotificationBuilderMock.setContentTitle(anyString())).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.setContentText(anyString())).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.setWhen(anyLong())).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.build()).thenReturn(mNotificationMock);
        Notification.BigTextStyle bigTextStyle = mock(Notification.BigTextStyle.class);
        whenNew(Notification.BigTextStyle.class).withAnyArguments().thenReturn(bigTextStyle);
        when(bigTextStyle.bigText(any(CharSequence.class))).thenReturn(bigTextStyle);
        when(mNotificationBuilderMock.getNotification()).thenReturn(mNotificationMock);
        whenNew(Notification.Builder.class).withArguments(mContextMock).thenReturn(mNotificationBuilderMock);
        mApplicationInfoMock = new ApplicationInfo();
        mApplicationInfoMock.icon = mIconId;
        mApplicationInfoMock.targetSdkVersion = Build.VERSION_CODES.O;
        when(mContextMock.getPackageManager()).thenReturn(mPackageManagerMock);
        when(mContextMock.getApplicationContext()).thenReturn(mContextMock);
        when(mContextMock.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManagerMock);
        when(mContextMock.getApplicationInfo()).thenReturn(mApplicationInfoMock);
        when(mPackageManagerMock.getLaunchIntentForPackage(anyString())).thenReturn(mActionIntentMock);
        when(mPackageManagerMock.getApplicationInfo(anyString(), eq(PackageManager.GET_META_DATA))).thenReturn(mApplicationInfoMock);
        mockStatic(Color.class);
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty(any(CharSequence.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence str = (CharSequence) invocation.getArguments()[0];
                return str == null || str.length() == 0;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        setVersionSdkInt(0);
    }

    @Test
    public void coverInit() {
        new PushNotifier();
    }

    @Test
    public void handlePushWithNoMessageId() {
        when(PushIntentUtils.getMessageId(any(Intent.class))).thenReturn(null);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationManagerMock).notify(anyInt(), any(Notification.class));
        verifyStatic();
        PushIntentUtils.setMessageId(anyString(), same(mActionIntentMock));
    }

    @Test
    public void handlePushBigText() {
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationManagerMock).notify(anyInt(), any(Notification.class));
        verifyStatic();
        PushIntentUtils.setMessageId(eq(mDummyGoogleMessageId), same(mActionIntentMock));

        /* Verify we apply big text style. */
        ArgumentCaptor<Notification.BigTextStyle> styleArgumentCaptor = ArgumentCaptor.forClass(Notification.BigTextStyle.class);
        verify(mNotificationBuilderMock).setStyle(styleArgumentCaptor.capture());
        verify(styleArgumentCaptor.getValue()).bigText("message");
    }

    @Test
    public void handlePushTargetN() {
        mApplicationInfoMock.targetSdkVersion = Build.VERSION_CODES.N;
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationManagerMock).notify(anyInt(), any(Notification.class));
        verifyStatic();
        PushIntentUtils.setMessageId(eq(mDummyGoogleMessageId), same(mActionIntentMock));
    }

    @SuppressLint("InlinedApi")
    @Test
    public void handleNotificationWithColor() {
        String colorString = "#331144";
        when(PushIntentUtils.getColor(any(Intent.class))).thenReturn(colorString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verifyStatic();
        Color.parseColor(eq(colorString));
        verify(mNotificationBuilderMock).setColor(anyInt());
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithInvalidColor() {
        String colorString = "invalid";
        when(PushIntentUtils.getColor(any(Intent.class))).thenReturn(colorString);
        when(Color.parseColor(anyString())).thenThrow(new IllegalArgumentException("Unknown color"));
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock, never()).setColor(anyInt());
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @SuppressLint("InlinedApi")
    @Test
    public void handleNotificationWithColorKitKat() throws Exception {
        setVersionSdkInt(Build.VERSION_CODES.KITKAT);
        String colorString = "#331144";
        when(PushIntentUtils.getColor(any(Intent.class))).thenReturn(colorString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock, never()).setColor(anyInt());
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithTitle() {
        String title = "title";
        when(PushIntentUtils.getTitle(any(Intent.class))).thenReturn(title);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setContentTitle(title);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithNullTitle() {
        String appName = "app-name";
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContextMock)).thenReturn(appName);
        when(PushIntentUtils.getTitle(any(Intent.class))).thenReturn(null);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setContentTitle(appName);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithEmptyTitle() {
        String appName = "app-name";
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContextMock)).thenReturn(appName);
        when(PushIntentUtils.getTitle(any(Intent.class))).thenReturn("");
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setContentTitle(appName);
    }

    @Test
    public void handleNotificationWithIconFromDrawable() {
        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(resourceId);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId + 1);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(resourceId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithIconFromMipmap() {
        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(resourceId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithIconFromMipmapAdaptiveAndroid26() throws Exception {

        /* Adaptive icon makes notification system crash in a loop on Android 8.0. */
        setVersionSdkInt(Build.VERSION_CODES.O);
        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        when(mContextMock.getDrawable(resourceId)).thenReturn(mock(AdaptiveIconDrawable.class));
        PushNotifier.handleNotification(mContextMock, new Intent());

        /* Fall back on app icon. */
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(mIconId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithAppIcon() {
        String iconString = "picture";
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(0);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(mIconId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithInvalidAppIcon() {
        mApplicationInfoMock.icon = 0;
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setSmallIcon(R.drawable.ic_stat_notify_dot);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithAppIconAdaptiveAndroid26() throws Exception {

        /* Adaptive icon makes notification system crash in a loop on Android 8.0. */
        setVersionSdkInt(Build.VERSION_CODES.O);
        String iconString = "picture";
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(0);
        when(mContextMock.getDrawable(mIconId)).thenReturn(mock(AdaptiveIconDrawable.class));
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(R.drawable.ic_stat_notify_dot);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithAppIconAdaptiveAndroid27() throws Exception {

        /* Adaptive icons are not a problem starting Android 8.1, so don't use fallback. */
        setVersionSdkInt(Build.VERSION_CODES.O_MR1);
        String iconString = "picture";
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(0);
        when(mContextMock.getDrawable(mIconId)).thenReturn(mock(AdaptiveIconDrawable.class));
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(mIconId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithIconFromMipmapAndAppIconAdaptiveAndroid26() throws Exception {

        /* Adaptive icon makes notification system crash in a loop on Android 8.0. */
        setVersionSdkInt(Build.VERSION_CODES.O);
        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        when(mContextMock.getDrawable(anyInt())).thenReturn(mock(AdaptiveIconDrawable.class));
        PushNotifier.handleNotification(mContextMock, new Intent());

        /* Fall back on dot as both app icon and custom icon are adaptive and Android 8. */
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(R.drawable.ic_stat_notify_dot);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithCustomData() {
        Map<String, String> customData = new HashMap<>();
        customData.put("key", "val");
        customData.put("key2", "val2");
        when(PushIntentUtils.getCustomData(any(Intent.class))).thenReturn(customData);
        PushNotifier.handleNotification(mContextMock, new Intent());
        for (String key : customData.keySet()) {
            verify(mActionIntentMock).putExtra(key, customData.get(key));
        }
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithCustomSound() throws Exception {
        String customSoundString = "sound";
        int resourceId = 2;
        when(PushIntentUtils.getSound(any(Intent.class))).thenReturn(customSoundString);
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(customSoundString), eq("raw"), anyString())).thenReturn(resourceId);
        when(resourcesMock.getResourcePackageName(resourceId)).thenReturn("packageName");
        when(resourcesMock.getResourceTypeName(resourceId)).thenReturn("typeName");
        when(resourcesMock.getResourceEntryName(resourceId)).thenReturn("entryName");
        Uri.Builder uriBuilderMock = mock(Uri.Builder.class);
        Uri uriMock = mock(Uri.class);
        whenNew(Uri.Builder.class).withNoArguments().thenReturn(uriBuilderMock);
        when(uriBuilderMock.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)).thenReturn(uriBuilderMock);
        when(uriBuilderMock.authority(resourcesMock.getResourcePackageName(resourceId))).thenReturn(uriBuilderMock);
        when(uriBuilderMock.appendPath(resourcesMock.getResourceTypeName(resourceId))).thenReturn(uriBuilderMock);
        when(uriBuilderMock.appendPath(resourcesMock.getResourceEntryName(resourceId))).thenReturn(uriBuilderMock);
        when(uriBuilderMock.build()).thenReturn(uriMock);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setSound(uriMock);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithInvalidCustomSound() {
        String customSoundString = "default";
        int resourceId = 2;
        when(PushIntentUtils.getSound(any(Intent.class))).thenReturn(customSoundString);
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(customSoundString), eq("raw"), anyString())).thenThrow(new Resources.NotFoundException());
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setDefaults(Notification.DEFAULT_SOUND);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithLauncherActivityHasRightFlags() {
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mActionIntentMock).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithNoLauncherActivity() throws Exception {

        /* Override set up mock to return null for launcher activity. */
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withNoArguments().thenReturn(intent);
        when(mPackageManagerMock.getLaunchIntentForPackage(anyString())).thenReturn(null);
        String title = "title";
        when(PushIntentUtils.getTitle(any(Intent.class))).thenReturn(title);
        Map<String, String> customData = new HashMap<>();
        customData.put("key", "val");
        customData.put("key2", "val2");
        when(PushIntentUtils.getCustomData(any(Intent.class))).thenReturn(customData);
        PushNotifier.handleNotification(mContextMock, new Intent("mock"));
        verify(mNotificationBuilderMock).setContentTitle(title);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);

        /* Intent being invalid, we don't put extras in that case, it will never be listened. */
        verify(intent, never()).putExtra(anyString(), anyString());
    }

    @Test
    public void handleNotificationWithDefaultIcon() {
        int resourceId = 42;
        Bundle bundle = mock(Bundle.class);
        when(bundle.getInt(DEFAULT_ICON_METADATA_NAME)).thenReturn(resourceId);
        mApplicationInfoMock.metaData = bundle;
        PushNotifier.handleNotification(mContextMock, new Intent());
        //noinspection ResourceType
        verify(mNotificationBuilderMock).setSmallIcon(resourceId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithDefaultColor() {
        int resourceId = 42;
        int color = 0x424242;
        Bundle bundle = mock(Bundle.class);
        when(bundle.getInt(DEFAULT_COLOR_METADATA_NAME)).thenReturn(resourceId);
        mApplicationInfoMock.metaData = bundle;
        when(mContextMock.getColor(eq(resourceId))).thenReturn(color);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setColor(eq(color));
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationWithDefaultColorLollipop() throws Exception {
        setVersionSdkInt(Build.VERSION_CODES.LOLLIPOP_MR1);
        int resourceId = 42;
        int color = 0x424242;
        Bundle bundle = mock(Bundle.class);
        when(bundle.getInt(DEFAULT_COLOR_METADATA_NAME)).thenReturn(resourceId);
        mApplicationInfoMock.metaData = bundle;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getColor(eq(resourceId))).thenReturn(color);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setColor(eq(color));
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @Test
    public void handleNotificationMetadataPackageNotFound() throws PackageManager.NameNotFoundException {
        when(mPackageManagerMock.getApplicationInfo(anyString(), eq(PackageManager.GET_META_DATA)))
                .thenThrow(new PackageManager.NameNotFoundException());
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }
}
