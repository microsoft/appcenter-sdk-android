/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import static com.microsoft.appcenter.utils.storage.SharedPreferencesManager.PREFERENCES_NAME;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SharedPreferencesManagerTest {

    @Mock
    private Context mContext;

    @Mock
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        SharedPreferencesManager.sSharedPreferences = null;
    }

    @Test
    public void initialize() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);

        SharedPreferencesManager.initialize(mContext);

        verify(mContext).getSharedPreferences(eq(PREFERENCES_NAME), eq(Context.MODE_PRIVATE));
        verifyNoMoreInteractions(mContext);
    }

    @Test
    public void initializeProtectedStorageContext() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N);

        Context protectedStorageContext = mock(Context.class);
        when(mContext.createDeviceProtectedStorageContext()).thenReturn(protectedStorageContext);
        when(protectedStorageContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);

        SharedPreferencesManager.initialize(mContext);

        verify(mContext).createDeviceProtectedStorageContext();
        verifyNoMoreInteractions(mContext);
        verify(protectedStorageContext).getSharedPreferences(eq(PREFERENCES_NAME), eq(Context.MODE_PRIVATE));
        verifyNoMoreInteractions(protectedStorageContext);
    }
}