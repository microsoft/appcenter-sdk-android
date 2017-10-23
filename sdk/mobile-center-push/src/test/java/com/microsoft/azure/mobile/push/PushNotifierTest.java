package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static android.content.Context.NOTIFICATION_SERVICE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({PushIntentUtils.class, PushNotifier.class, Context.class})
public class PushNotifierTest {

    private String mDummyGoogleMessageId = "messageId";
    private Context mContextMock;
    private NotificationManager mNotificationManagerMock;
    private Notification mNotificationMock;
    private Notification.Builder mNotificationBuilderMock;
    private int mIconId = 29;

    @Before
    public void setUp() throws Exception {
        mockStatic(PushIntentUtils.class);
        when(PushIntentUtils.getMessage(any(Intent.class))).thenReturn("message");
        when(PushIntentUtils.getGoogleMessageId(any(Intent.class))).thenReturn(mDummyGoogleMessageId);
        when(PushIntentUtils.getCustomData(any(Intent.class))).thenReturn(new HashMap<String, String>());

        mContextMock = mock(Context.class);
        mNotificationManagerMock = mock(NotificationManager.class);
        mNotificationMock = mock(Notification.class);


        mNotificationBuilderMock = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withArguments(mContextMock).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.setContentTitle(anyString())).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.setContentText(anyString())).thenReturn(mNotificationBuilderMock);
        when(mNotificationBuilderMock.setWhen(anyLong())).thenReturn(mNotificationBuilderMock);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            when(mNotificationBuilderMock.build()).thenReturn(mNotificationMock);
        }
        when(mNotificationBuilderMock.getNotification()).thenReturn(mNotificationMock);
        PackageManager packageManagerMock = mock(PackageManager.class);
        when(packageManagerMock.getLaunchIntentForPackage(anyString())).thenReturn(new Intent());
        when(mContextMock.getPackageManager()).thenReturn(packageManagerMock);
        when(mContextMock.getApplicationContext()).thenReturn(mContextMock);
        when(mContextMock.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManagerMock);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.icon = mIconId;
        when(mContextMock.getApplicationInfo()).thenReturn(applicationInfo);
    }

    @SuppressLint("InlinedApi")
    @Test
    public void handleNotificationWithColor() throws Exception {
        String colorString = "#331144";
        when(PushIntentUtils.getColor(any(Intent.class))).thenReturn(colorString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setColor(Color.parseColor(colorString));
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @SuppressLint("InlinedApi")
    @Test
    public void handleNotificationWithTitle() throws Exception {
        String title = "title";
        when(PushIntentUtils.getTitle(any(Intent.class))).thenReturn(title);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setContentTitle(title);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void handleNotificationWithIconFromDrawable() throws Exception {

        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(resourceId);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId + 1);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setSmallIcon(resourceId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void handleNotificationWithIconFromMipmap() throws Exception {

        String iconString = "picture";
        int resourceId = 4;
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(resourceId);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setSmallIcon(resourceId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void handleNotificationWithAppIcon() throws Exception {

        String iconString = "picture";
        Resources resourcesMock = mock(Resources.class);
        when(mContextMock.getResources()).thenReturn(resourcesMock);
        when(resourcesMock.getIdentifier(eq(iconString), eq("drawable"), anyString())).thenReturn(0);
        when(resourcesMock.getIdentifier(eq(iconString), eq("mipmap"), anyString())).thenReturn(0);
        when(PushIntentUtils.getIcon(any(Intent.class))).thenReturn(iconString);
        PushNotifier.handleNotification(mContextMock, new Intent());
        verify(mNotificationBuilderMock).setSmallIcon(mIconId);
        verify(mNotificationManagerMock).notify(mDummyGoogleMessageId.hashCode(), mNotificationMock);
    }
}
