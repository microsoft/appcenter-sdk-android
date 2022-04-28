/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({
        PermissionUtils.class,
        Context.class
})
public class PermissionUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    Context mContext;

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void init() {
        new PermissionUtils();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void permissionsStateNull() {
        int[] permissionsState = PermissionUtils.permissionsState(mContext, (String[]) null);
        assertNull(permissionsState);
    }

    @Test
    public void permissionsState() {
        when(mContext.checkCallingOrSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        String[] strings = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int[] permissionsState = PermissionUtils.permissionsState(mContext, strings);
        assertEquals(strings.length, permissionsState.length);
        verify(mContext, times(strings.length)).checkCallingOrSelfPermission(anyString());
    }

    @Test
    public void permissionsAreGranted() {
        int[] permissionsState = new int[]{
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED
        };
        assertFalse(PermissionUtils.permissionsAreGranted(permissionsState));
        int[] grantedPermissions = new int[]{
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED
        };
        assertTrue(PermissionUtils.permissionsAreGranted(grantedPermissions));
    }
}