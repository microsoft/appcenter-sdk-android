/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.INSTALL_STATUS_ACTION;
import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.MY_PACKAGE_REPLACED_ACTION;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({
        AppCenterLog.class,
        AppNameHelper.class,
        Distribute.class,
        DistributeUtils.class
})
@RunWith(PowerMockRunner.class)
public class AppCenterPackageInstallerReceiverTest {

    @Mock
    private Context mContext;

    @Mock
    private Intent mIntent;

    @Mock
    private Bundle mExtra;

    private AppCenterPackageInstallerReceiver mAppCenterPackageInstallerReceiver;

    private void installStatus(int status) {
        when(mIntent.getAction()).thenReturn(INSTALL_STATUS_ACTION);
        when(mExtra.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
    }

    @Before
    public void setUp() {
        when(mIntent.getExtras()).thenReturn(mExtra);

        /* Mock static classes. */
        mockStatic(AppCenterLog.class);
        mockStatic(DistributeUtils.class);

        /* Init receiver. */
        mAppCenterPackageInstallerReceiver = new AppCenterPackageInstallerReceiver();
    }

    @Test
    public void receiveMyPackageReplaced() {
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContext)).thenReturn("Contoso");
        Intent intent = mock(Intent.class);
        when(DistributeUtils.getResumeAppIntent(mContext)).thenReturn(intent);
        when(mIntent.getAction()).thenReturn(MY_PACKAGE_REPLACED_ACTION);
        when(mContext.getString(R.string.appcenter_distribute_install_completed_title))
                .thenReturn("Title");
        when(mContext.getString(R.string.appcenter_distribute_install_completed_message))
                .thenReturn("%1$s %2$s (%3$d)");

        /* Call method with MY_PACKAGE_REPLACED_ACTION action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        verifyStatic(DistributeUtils.class);
        DistributeUtils.postNotification(eq(mContext), anyString(), anyString(), eq(intent));
    }

    @Test
    public void receiveInstallStatusPendingUserAction() {
        installStatus(PackageInstaller.STATUS_PENDING_USER_ACTION);
        Intent confirmIntent = mock(Intent.class);
        when(mExtra.get(eq(Intent.EXTRA_INTENT))).thenReturn(confirmIntent);

        /* Call method with STATUS_PENDING_USER_ACTION action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that activity was started. */
        verify(mContext).startActivity(eq(confirmIntent));
    }

    @Test
    public void receiveInstallStatusSuccess() {
        installStatus(PackageInstaller.STATUS_SUCCESS);

        /* Call method with STATUS_SUCCESS action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.info(eq(LOG_TAG), eq("Application was successfully updated."));
    }

    @Test
    public void receiveInstallStatusFailure() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE);
    }

    @Test
    public void receiveInstallStatusFailureAborted() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_ABORTED);
    }

    @Test
    public void receiveInstallStatusFailureBlocked() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_BLOCKED);
    }

    @Test
    public void receiveInstallStatusFailureConflict() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_CONFLICT);
    }

    @Test
    public void receiveInstallStatusFailureIncompatible() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE);
    }

    @Test
    public void receiveInstallStatusFailureInvalid() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_INVALID);
    }

    @Test
    public void receiveInstallStatusFailureStorage() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE_STORAGE);
    }

    private void receiveInstallStatusFailure(int status) {
        installStatus(status);

        mockStatic(Distribute.class);
        Distribute distribute = mock(Distribute.class);
        when(Distribute.getInstance()).thenReturn(distribute);

        /* Call method with failure action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verify(distribute).showInstallingErrorToast();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(LOG_TAG), eq("Failed to install a new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithUnrecognizedStatus() {
        final int UNKNOWN_STATUS = 42;
        installStatus(UNKNOWN_STATUS);

        /* Call method with wrong action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(LOG_TAG), eq("Unrecognized status received from installer: " + UNKNOWN_STATUS));
    }

    @Test
    public void onReceiverWithUnknownAction() {
        when(mIntent.getAction()).thenReturn("UnknownAction");

        /* Call method with wrong action. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(LOG_TAG), eq("Unrecognized action UnknownAction - do nothing."));
    }
}

