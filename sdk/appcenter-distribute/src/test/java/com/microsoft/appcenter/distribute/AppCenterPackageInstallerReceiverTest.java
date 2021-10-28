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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({
        AppCenterLog.class,
        Toast.class
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
    private PackageInstaller mPackageInstaller;

    @Mock
    private Bundle mBundle;

    @Mock
    private Intent mConfirmIntent;

    @Mock
    private Toast mToast;

    private AppCenterPackageInstallerReceiver mAppCenterPackageInstallerReceiver;

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
        mockStatic(Toast.class);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("com.mock.package");

        mAppCenterPackageInstallerReceiver = spy(new AppCenterPackageInstallerReceiver());
    }

    private void mockToast() {
        mockStatic(Toast.class);
        PowerMockito.when(Toast.makeText(any(Context.class), anyInt(), anyInt())).thenReturn(mToast);
    }

    @Test
    public void onReceiveWithActionMyPackageReplace() {
        when(mIntent.getAction()).thenReturn(MY_PACKAGE_REPLACED_ACTION);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mock(Intent.class));

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verify(mContext).startActivity(Matchers.<Intent>any());
    }

    @Test
    public void onReceiveWithStartIntentWithStatusPendingUserAction() {
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(PackageInstaller.STATUS_PENDING_USER_ACTION);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verify(mContext).startActivity(mConfirmIntent);
    }

    @Test
    public void onReceiveWithStartIntentWithStatusSuccess() {
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(PackageInstaller.STATUS_SUCCESS);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Application was successfully updated."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailure() {
        int status = PackageInstaller.STATUS_FAILURE;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureAborted() {
        int status = PackageInstaller.STATUS_FAILURE_ABORTED;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureBlocked() {
        int status = PackageInstaller.STATUS_FAILURE_BLOCKED;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureConflict() {
        int status = PackageInstaller.STATUS_FAILURE_CONFLICT;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureIncompatible() {
        int status = PackageInstaller.STATUS_FAILURE_INCOMPATIBLE;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureInvalid() {
        int status = PackageInstaller.STATUS_FAILURE_INVALID;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
    }

    @Test
    public void onReceiveWithStartIntentWithStatusFailureStorage() {
        int status = PackageInstaller.STATUS_FAILURE_STORAGE;
        when(mIntent.getAction()).thenReturn(START_ACTION);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(status);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);
        when(Toast.makeText(any(Context.class), eq("Failed during installing new release."), anyInt())).thenReturn(mToast);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Failed to install new release with status: " + status + ". Error message: null."));
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

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("Unrecognized status received from installer: " + status));
    }
    }

