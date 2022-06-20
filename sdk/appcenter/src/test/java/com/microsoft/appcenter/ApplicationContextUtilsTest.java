/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class ApplicationContextUtilsTest {

    @Mock
    private Context mDeviceProtectedStorageContext;

    @Mock
    private Application mApplication;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        when(mApplication.getApplicationContext()).thenReturn(mApplication);
        when(mApplication.isDeviceProtectedStorage()).thenReturn(false);
        when(mApplication.createDeviceProtectedStorageContext()).thenReturn(mDeviceProtectedStorageContext);
        when(mApplication.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mDeviceProtectedStorageContext.isDeviceProtectedStorage()).thenReturn(true);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void constructorCoverage() {
        new ApplicationContextUtils();
    }

    @Test
    public void contextOnNewLockedDevice() {

        /* Mock SDK_INT to N. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* When user is locked. */
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        /* It should be device-protected storage context. */
        assertEquals(mDeviceProtectedStorageContext, ApplicationContextUtils.getApplicationContext(mApplication));

        /* Verify create protected storage. */
        verify(mApplication).createDeviceProtectedStorageContext();
    }

    @Test
    public void contextOnNewUnlockedDevice() {

        /* Mock SDK_INT to N. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* When user is unlocked. */
        when(mUserManager.isUserUnlocked()).thenReturn(true);

        /* It should be regular context. */
        assertEquals(mApplication, ApplicationContextUtils.getApplicationContext(mApplication));

        /* Verify get application context. */
        verify(mApplication).getApplicationContext();
    }

    @Test
    public void contextOnOldDevice() {

        /* Mock SDK_INT to M. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        /* When user is locked. */
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        /* It should be regular context. */
        assertEquals(mApplication, ApplicationContextUtils.getApplicationContext(mApplication));

        /* Verify get application context. */
        verify(mApplication).getApplicationContext();
    }

    @Test
    public void checkDeviceProtectedStorageOnNewDevice() {

        /* Mock SDK_INT to N. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* It should rely on context. */
        assertTrue(ApplicationContextUtils.isDeviceProtectedStorage(mDeviceProtectedStorageContext));
        assertFalse(ApplicationContextUtils.isDeviceProtectedStorage(mApplication));
    }

    @Test
    public void checkDeviceProtectedStorageOnOldDevice() {

        /* Mock SDK_INT to M. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        /* It cannot be device-protected storage on old devices. */
        assertFalse(ApplicationContextUtils.isDeviceProtectedStorage(mApplication));
        verifyNoInteractions(mApplication);
    }

    @After
    public void tearDown() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
