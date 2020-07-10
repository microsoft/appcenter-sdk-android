/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Context;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class DistributeTest {

    @Before
    public void setUp() {
        new DistributeConstants();
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void isEnabledTest() {
        Distribute.isEnabled();
        verifyCallAppCenterLog("Called method 'isEnabled'");
    }

    @Test
    public void setEnabledTest() {
        Distribute.setEnabled(true);
        verifyCallAppCenterLog("Called method 'setEnabled'");
    }

    @Test
    public void setInstallUrlTest() {
        Distribute.setInstallUrl("");
        verifyCallAppCenterLog("Called method 'setInstallUrl'");
    }

    @Test
    public void setApiUrlTest() {
        Distribute.setApiUrl("");
        verifyCallAppCenterLog("Called method 'setApiUrl'");
    }

    @Test
    public void getUpdateTrackTest() {
        Distribute.getUpdateTrack();
        verifyCallAppCenterLog("Called method 'getUpdateTrack'");
    }

    @Test
    public void setUpdateTrackTest() {
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        verifyCallAppCenterLog("Called method 'setUpdateTrack'");
    }

    @Test
    public void setListenerTest() {
        DistributeListener mockListener = mock(DistributeListener.class);
        Activity mockActivity = mock(Activity.class);
        ReleaseDetails mockRelease = mock(ReleaseDetails.class);
        Distribute.setListener(mockListener);
        mockListener.onReleaseAvailable(mockActivity, mockRelease);
    }

    @Test
    public void setEnabledForDebuggableBuildTest() {
        Distribute.setEnabledForDebuggableBuild(true);
        verifyCallAppCenterLog("Called method 'setEnabledForDebuggableBuild'");
    }

    @Test
    public void notifyUpdateActionTest() {
        Distribute.notifyUpdateAction(UpdateAction.UPDATE);
        verifyCallAppCenterLog("Called method 'notifyUpdateAction'");
    }

    @Test
    public void checkForUpdateTest() {
       Distribute.checkForUpdate();
       verifyCallAppCenterLog("Called method 'checkForUpdate'");
    }

    @Test
    public void disableAutomaticCheckForUpdateTest() {
        Distribute.disableAutomaticCheckForUpdate();
        verifyCallAppCenterLog("Called method 'disableAutomaticCheckForUpdate'");
    }

    @Test
    public void getGroupNameTest() {
        Distribute.getInstance().getGroupName();
        verifyCallAppCenterLog("Called method 'getGroupName'");
    }

    @Test
    public void getServiceNameTest() {
        Distribute.getInstance().getServiceName();
        verifyCallAppCenterLog("Called method 'getServiceName'");
    }

    @Test
    public void getLoggerTagTest() {
        Distribute.getInstance().getLoggerTag();
        verifyCallAppCenterLog("Called method 'getLoggerTag'");
    }

    @Test
    public void getTriggerCountTest() {
        Distribute.getInstance().getTriggerCount();
        verifyCallAppCenterLog("Called method 'getTriggerCount'");
    }

    @Test
    public void getLogFactoriesTest() {
        Distribute.getInstance().getLogFactories();
        verifyCallAppCenterLog("Called method 'getLogFactories'");
    }

    @Test
    public void onStartedTest() {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        Distribute.getInstance().onStarted(mockContext, mockChannel, "app-secret", "token", true);
        verifyCallAppCenterLog("Called method 'onStarted'");
    }

    @Test
    public void onActivityResumedTest() {
        Activity mockActivity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(mockActivity);
        verifyCallAppCenterLog("Called method 'onActivityResumed'");
    }

    @Test
    public void onActivityPausedTest() {
        Activity mockActivity = mock(Activity.class);
        Distribute.getInstance().onActivityPaused(mockActivity);
        verifyCallAppCenterLog("Called method 'onActivityPaused'");
    }

    @Test
    public void onApplicationEnterForegroundTest() {
        Distribute.getInstance().onApplicationEnterForeground();
        verifyCallAppCenterLog("Called method 'onApplicationEnterForeground'");
    }

    @Test
    public void applyEnabledStateTest() {
        Distribute.getInstance().applyEnabledState(true);
        verifyCallAppCenterLog("Called method 'applyEnabledState'");
    }

    private void verifyCallAppCenterLog(String str) {
        verifyStatic();
        AppCenterLog.debug(anyString(), eq(str));
    }
}
