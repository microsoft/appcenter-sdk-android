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
    }

    @Test
    public void setEnabledTest() {
        Distribute.setEnabled(true);
    }

    @Test
    public void setInstallUrlTest() {
        Distribute.setInstallUrl("");
    }

    @Test
    public void setApiUrlTest() {
        Distribute.setApiUrl("");
    }

    @Test
    public void getUpdateTrackTest() {
        Distribute.getUpdateTrack();
    }

    @Test
    public void setUpdateTrackTest() {
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
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
    }

    @Test
    public void notifyUpdateActionTest() {
        Distribute.notifyUpdateAction(UpdateAction.UPDATE);
    }

    @Test
    public void checkForUpdateTest() {
       Distribute.checkForUpdate();
    }

    @Test
    public void disableAutomaticCheckForUpdateTest() {
        Distribute.disableAutomaticCheckForUpdate();
    }

    @Test
    public void getGroupNameTest() {
        Distribute.getInstance().getGroupName();
    }

    @Test
    public void getServiceNameTest() {
        Distribute.getInstance().getServiceName();
    }

    @Test
    public void getLoggerTagTest() {
        Distribute.getInstance().getLoggerTag();
    }

    @Test
    public void getTriggerCountTest() {
        Distribute.getInstance().getTriggerCount();
    }

    @Test
    public void getLogFactoriesTest() {
        Distribute.getInstance().getLogFactories();
    }

    @Test
    public void onStartedTest() {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        Distribute.getInstance().onStarted(mockContext, mockChannel, "app-secret", "token", true);
    }

    @Test
    public void onActivityResumedTest() {
        Activity mockActivity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(mockActivity);
    }

    @Test
    public void onActivityPausedTest() {
        Activity mockActivity = mock(Activity.class);
        Distribute.getInstance().onActivityPaused(mockActivity);
    }

    @Test
    public void onApplicationEnterForegroundTest() {
        Distribute.getInstance().onApplicationEnterForeground();
    }

    @Test
    public void applyEnabledStateTest() {
        Distribute.getInstance().applyEnabledState(true);
    }

}
