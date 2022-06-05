/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.install.session.InstallStatusReceiver.INSTALL_STATUS_ACTION;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
        AppCenterLog.class,
        DeviceInfoHelper.class,
        PendingIntent.class
})
@RunWith(PowerMockRunner.class)
public class InstallStatusReceiverTest {

    private static final int SESSION_ID = 42;

    @Mock
    private Context mContext;

    @Mock
    private Intent mIntent;

    @Mock
    private Bundle mExtra;

    @Mock
    private SessionReleaseInstaller mInstaller;

    private InstallStatusReceiver mInstallStatusReceiver;

    private void installStatus(int status) {
        when(mIntent.getAction()).thenReturn(INSTALL_STATUS_ACTION);
        when(mExtra.getInt(eq(PackageInstaller.EXTRA_SESSION_ID))).thenReturn(SESSION_ID);
        when(mExtra.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
    }

    @Before
    public void setUp() {
        when(mIntent.getExtras()).thenReturn(mExtra);

        /* Mock static classes. */
        mockStatic(AppCenterLog.class);
        mockStatic(DeviceInfoHelper.class);

        /* Init receiver. */
        mInstallStatusReceiver = new InstallStatusReceiver(mInstaller);
    }

    @Test
    public void receiveInstallStatusPendingUserAction() {
        installStatus(PackageInstaller.STATUS_PENDING_USER_ACTION);
        Intent confirmIntent = mock(Intent.class);
        when(mExtra.getParcelable(eq(Intent.EXTRA_INTENT))).thenReturn(confirmIntent);

        /* Call method with STATUS_PENDING_USER_ACTION action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* Verify callback has been passed through. */
        verify(mInstaller).onInstallConfirmation(eq(SESSION_ID), eq(confirmIntent));
        verifyNoMoreInteractions(mInstaller);
    }

    @Test
    public void receiveInstallStatusSuccess() {
        installStatus(PackageInstaller.STATUS_SUCCESS);

        /* Call method with STATUS_SUCCESS action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* No-op in this case, just make sure that it's logged. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.info(eq(LOG_TAG), anyString());
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void receiveInstallStatusFailureAborted() {
        installStatus(PackageInstaller.STATUS_FAILURE_ABORTED);

        /* Call method with failure action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* Verify callback has been passed through. */
        verify(mInstaller).onInstallCancel(eq(SESSION_ID));
        verifyNoMoreInteractions(mInstaller);
    }

    @Test
    public void receiveInstallStatusFailure() {
        receiveInstallStatusFailure(PackageInstaller.STATUS_FAILURE);
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
        String errorMessage = "Some error message, status=" + status;
        when(mExtra.getString(eq(PackageInstaller.EXTRA_STATUS_MESSAGE))).thenReturn(errorMessage);

        /* Call method with failure action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* Verify callback has been passed through. */
        verify(mInstaller).onInstallError(eq(SESSION_ID), eq(errorMessage));
        verifyNoMoreInteractions(mInstaller);
    }

    @Test
    public void onReceiveWithStartIntentWithUnrecognizedStatus() {
        final int UNKNOWN_STATUS = 42;
        installStatus(UNKNOWN_STATUS);

        /* Call method with wrong action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* Verify that log was called. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(LOG_TAG), anyString());
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void onReceiverWithUnknownAction() {
        when(mIntent.getAction()).thenReturn("UnknownAction");

        /* Call method with wrong action. */
        mInstallStatusReceiver.onReceive(mContext, mIntent);

        /* No-op in this case. */
        verifyNoInteractions(mInstaller);
    }

    @Test
    public void createIntentSenderOnAndroidS() {

        /* Mock SDK_INT to S. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.S);
        createIntentSender(PendingIntent.FLAG_MUTABLE);
    }

    @Test
    public void createIntentSenderOnAndroidLowS() {

        /* Mock SDK_INT to M. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        createIntentSender(0);
    }

    private void createIntentSender(int expectedFlag) {
        mockStatic(PendingIntent.class);
        PendingIntent intent = mock(PendingIntent.class);
        IntentSender sender = mock(IntentSender.class);
        when(intent.getIntentSender()).thenReturn(sender);
        when(PendingIntent.getBroadcast(any(Context.class), eq(SESSION_ID), any(Intent.class), eq(expectedFlag)))
                .thenReturn(intent);

        /* Getting install status intent sender. */
        assertEquals(sender, InstallStatusReceiver.getInstallStatusIntentSender(mContext, SESSION_ID));
    }
}
