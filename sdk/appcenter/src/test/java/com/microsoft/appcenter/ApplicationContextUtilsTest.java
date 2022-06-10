/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
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
    private Context mProtectedContext;

    @Mock
    private Application mApplication;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        when(mApplication.getApplicationContext()).thenReturn(mApplication);
        when(mApplication.createDeviceProtectedStorageContext()).thenReturn(mProtectedContext);
        when(mApplication.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void ConstructorCoverage() {
        new ApplicationContextUtils();
    }

    @Test
    public void getContextWhenVersionIsHigherOrEqualThanNAndUnlockedIsFalse() {

        /* Mock SDK_INT to N. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* When user is locked. */
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        /* Method call. */
        Context result = ApplicationContextUtils.getApplicationContext(mApplication);

        /* Compare of two results. */
        assertEquals(mProtectedContext, result);

        /* Verify create protected storage. */
        verify(mApplication).createDeviceProtectedStorageContext();
    }

    @Test
    public void getContextWhenVersionIsHigherOrEqualThanNAndUnlockedIsTrue() {

        /* Mock SDK_INT to N. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        /* When user is unlocked. */
        when(mUserManager.isUserUnlocked()).thenReturn(true);

        /* Method call. */
        Context result = ApplicationContextUtils.getApplicationContext(mApplication);

        /* Compare of two results. */
        assertEquals(mApplication, result);

        /* Verify get application context. */
        verify(mApplication).getApplicationContext();
    }

    @Test
    public void getContextWhenVersionIsLowerThanN() {

        /* Mock SDK_INT to M. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        /* When user is locked. */
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        /* Method call. */
        Context result = ApplicationContextUtils.getApplicationContext(mApplication);

        /* Compare of two results. */
        assertEquals(mApplication, result);

        /* Verify get application context. */
        verify(mApplication).getApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
