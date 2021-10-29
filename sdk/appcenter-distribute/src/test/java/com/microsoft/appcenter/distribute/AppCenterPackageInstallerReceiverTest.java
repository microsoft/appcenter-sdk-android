/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.MY_PACKAGE_REPLACED_ACTION;
import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.START_ACTION;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Locale;

@PrepareForTest({
        AppCenterLog.class,
        Toast.class,
        AppCenterPackageInstallerReceiver.class
})
public class AppCenterPackageInstallerReceiverTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Intent mIntent;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private Bundle mBundle;

    @Mock
    private Intent mConfirmIntent;

    @Mock
    private Toast mToast;

    private AppCenterPackageInstallerReceiver mAppCenterPackageInstallerReceiver;

    @Before
    public void setUp() {

        /* Mock static classes. */
        mockStatic(AppCenterLog.class);
        mockStatic(Toast.class);

        /* Mock methods. */
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("com.mock.package");

        /* Mock toast. */
        when(Toast.makeText(any(Context.class), anyString(), anyInt())).thenReturn(mToast);

        /* Init receiver. */
        mAppCenterPackageInstallerReceiver = spy(new AppCenterPackageInstallerReceiver());
    }

    @After
    public void cleanUp() {
        mAppCenterPackageInstallerReceiver = null;
    }

    @Test
    public void onReceiveWithActionMyPackageReplace() {
        when(mIntent.getAction()).thenReturn(MY_PACKAGE_REPLACED_ACTION);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mock(Intent.class));

        /* Call method with MY_PACKAGE_REPLACED_ACTION action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that activity was started. */
        verify(mContext).startActivity(Matchers.<Intent>any());
    }

    @Test
    public void onReceiveWithStartIntentWithStatusPendingUserAction() {
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(PackageInstaller.STATUS_PENDING_USER_ACTION);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Call method with STATUS_PENDING_USER_ACTION action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that activity was started. */
        verify(mContext).startActivity(mConfirmIntent);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusSuccess() {
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(PackageInstaller.STATUS_SUCCESS);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Call method with STATUS_SUCCESS action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Application was successfully updated."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailure() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureAborted() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_ABORTED);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureBlocked() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_BLOCKED);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureConflict() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_CONFLICT);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureIncompatible() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureInvalid() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_INVALID);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureStorage() {
        onReceiveWithStartFailureAction(PackageInstaller.STATUS_FAILURE_STORAGE);
    }

    private void onReceiveWithStartFailureAction(int status) {
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Call method with failure action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install a new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithUnrecognizedStatus() {

        /* Create unrecognized status  */
        int status = 10;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Call method with wrong action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Unrecognized status received from installer: " + status));
    }

    @Test
    public void onReceiverWithUnknownAction() {
        when(mIntent.getAction()).thenReturn("UnknownAction");

        /* Call method with wrong action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Unrecognized action UnknownAction - do nothing."));
    }
}

