package com.microsoft.appcenter.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class AppNameHelperTest {

    @Test
    public void init() {
        assertNotNull(new AppNameHelper());
    }

    @Test
    public void localizedAppName() {
        String appName = "localized-app-name";
        int resId = 42;
        Context context = mock(Context.class);
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        when(context.getApplicationInfo()).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "labelRes", resId);
        when(context.getString(resId)).thenReturn(appName);
        String retrievedAppName = AppNameHelper.getAppName(context);
        assertEquals(appName, retrievedAppName);
    }

    @Test
    public void nonLocalizedAppName() {
        String appName = "non-localized-app-name";
        Context context = mock(Context.class);
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        when(context.getApplicationInfo()).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "labelRes", 0);
        Whitebox.setInternalState(applicationInfo, "nonLocalizedLabel", appName);
        String retrievedAppName = AppNameHelper.getAppName(context);
        assertEquals(appName, retrievedAppName);
    }
}