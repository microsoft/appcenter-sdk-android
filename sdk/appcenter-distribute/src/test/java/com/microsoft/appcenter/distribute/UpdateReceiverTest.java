/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.content.Intent.ACTION_MY_PACKAGE_REPLACED;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({
        AppNameHelper.class,
        DeviceInfoHelper.class,
        DistributeUtils.class
})
@RunWith(PowerMockRunner.class)
public class UpdateReceiverTest {

    @Mock
    private Context mContext;

    @Mock
    private Intent mIntent;

    @Mock
    private Intent mResumeIntent;

    private UpdateReceiver mUpdateReceiver;

    @Before
    public void setUp() {
        mockStatic(AppNameHelper.class);
        mockStatic(DeviceInfoHelper.class);
        mockStatic(DistributeUtils.class);

        when(DistributeUtils.getResumeAppIntent(mContext)).thenReturn(mResumeIntent);

        when(AppNameHelper.getAppName(mContext)).thenReturn("Contoso");

        when(mIntent.getAction()).thenReturn(ACTION_MY_PACKAGE_REPLACED);
        when(mContext.getString(R.string.appcenter_distribute_install_completed_title))
                .thenReturn("Title");
        when(mContext.getString(R.string.appcenter_distribute_install_completed_message))
                .thenReturn("%1$s %2$s (%3$d)");

        mUpdateReceiver = new UpdateReceiver();
    }

    @Test
    public void receiveMyPackageReplaced() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = "1.0";
        when(DeviceInfoHelper.getPackageInfo(mContext)).thenReturn(packageInfo);
        when(DeviceInfoHelper.getVersionCode(packageInfo)).thenReturn(1);

        /* Call method with ACTION_MY_PACKAGE_REPLACED action. */
        mUpdateReceiver.onReceive(mContext, mIntent);

        verifyStatic(DistributeUtils.class);
        DistributeUtils.postNotification(eq(mContext), eq("Title"), eq("Contoso 1.0 (1)"), eq(mResumeIntent));
    }

    @Test
    public void receiveMyPackageReplacedWithoutPackageInfo() {

        /* Call method with ACTION_MY_PACKAGE_REPLACED action. */
        mUpdateReceiver.onReceive(mContext, mIntent);

        verifyStatic(DistributeUtils.class);
        DistributeUtils.postNotification(eq(mContext), eq("Title"), eq("Contoso ? (0)"), eq(mResumeIntent));
    }
}