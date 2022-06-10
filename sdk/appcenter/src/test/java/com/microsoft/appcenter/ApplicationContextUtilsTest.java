/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class ApplicationContextUtilsTest {

    @Mock
    private Application mApplication;

    @Mock
    private UserManager mUserManager;

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void ConstructorCoverage() {
        new ApplicationContextUtils();
    }

    @Test
    public void getContextWhenVersionIsHigherOrEqualThenNAndUnlockedIsFalse() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        when(mApplication.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        ApplicationContextUtils.getApplicationContext(mApplication);

        verify(mApplication).createDeviceProtectedStorageContext();
    }

    @Test
    public void getContextWhenVersionIsHigherOrEqualThenNAndUnlockedIsTrue() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        when(mApplication.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mUserManager.isUserUnlocked()).thenReturn(true);

        ApplicationContextUtils.getApplicationContext(mApplication);

        verify(mApplication).getApplicationContext();
    }

    @Test
    public void getContextWhenVersionIsLowerThenN() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        when(mApplication.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mUserManager.isUserUnlocked()).thenReturn(false);

        ApplicationContextUtils.getApplicationContext(mApplication);

        verify(mApplication).getApplicationContext();
    }
}
