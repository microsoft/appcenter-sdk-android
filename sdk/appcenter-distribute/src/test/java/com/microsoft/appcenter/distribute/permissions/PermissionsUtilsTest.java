/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.permissions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest
public class PermissionsUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    public Context mContext;

    @Before
    public void setUp() {
        PermissionRequestActivity.sResultFuture = null;
    }

    @Test
    public void requestPermissions() {
        AppCenterFuture<PermissionRequestActivity.Result> future = PermissionUtils.requestPermissions(mContext, Manifest.permission.POST_NOTIFICATIONS);
        verify(mContext).startActivity(any());
        assertNotNull(future);
    }

    @Test
    public void requestPermissionsSecondTimeReturnNull() {
        AppCenterFuture<PermissionRequestActivity.Result> future = PermissionUtils.requestPermissions(mContext, Manifest.permission.POST_NOTIFICATIONS);
        assertNotNull(future);
        future = PermissionUtils.requestPermissions(mContext, Manifest.permission.POST_NOTIFICATIONS);
        assertNull(future);
    }
}
