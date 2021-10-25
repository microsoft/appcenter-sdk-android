package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.MY_PACKAGE_REPLACED_INTENT;
import static com.microsoft.appcenter.distribute.AppCenterPackageInstallerReceiver.START_INTENT;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

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

    private AppCenterPackageInstallerReceiver mAppCenterPackageInstallerReceiver;

    @Before
    public void setUp() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("com.mock.package");

        mAppCenterPackageInstallerReceiver = spy(new AppCenterPackageInstallerReceiver());
    }

    @Test
    public void onReceiveWithActionMyPackageReplace() {
        when(mIntent.getAction()).thenReturn(MY_PACKAGE_REPLACED_INTENT);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mock(Intent.class));

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verify(mContext).startActivity(Matchers.<Intent>any());
    }

    @Test
    public void onReceiveWithStartIntent() {
        when(mIntent.getAction()).thenReturn(START_INTENT);
        when(mIntent.getExtras()).thenReturn(mBundle);
        when(mBundle.getInt(eq(PackageInstaller.EXTRA_STATUS))).thenReturn(PackageInstaller.STATUS_PENDING_USER_ACTION);
        when(mBundle.get(eq(Intent.EXTRA_INTENT))).thenReturn(mConfirmIntent);

        /* Perform onReceive method invocation. */
        mAppCenterPackageInstallerReceiver.onReceive(mContext, mIntent);

        /* Verify. */
        verify(mContext).startActivity(mConfirmIntent);
    }
}
