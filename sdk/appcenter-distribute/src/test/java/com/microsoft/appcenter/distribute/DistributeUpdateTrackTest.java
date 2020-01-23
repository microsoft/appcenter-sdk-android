/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TRACK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Most of the public vs private scenarios were already tested in existing test classes
 * before we introduced the switch.
 * This test class focuses on the getter and switching the value.
 */
public class DistributeUpdateTrackTest extends AbstractDistributeTest {

    @Test
    public void getAndSetValueBeforeStart() {

        /* Check default value. */
        Assert.assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());

        /* Check changes. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
        Distribute.setUpdateTrack(UpdateTrack.PUBLIC);
        Assert.assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());
    }

    @Test
    public void setInvalidUpdateTrack() {

        /* Set a valid non default value. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Try set invalid values. */
        Distribute.setUpdateTrack(0);
        Distribute.setUpdateTrack(-1);
        Distribute.setUpdateTrack(42);

        /* Check value didn't change. */
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
    }

    @Test
    public void invalidPersistedTrackFallsBackToPublic() {

        /* Corrupt storage. */
        when(SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_UPDATE_TRACK), anyInt())).thenReturn(42);

        /* Start. */
        start();

        /* Check value didn't change. */
        Assert.assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());
    }

    @Test
    public void persistValueWhenDisabled() {

        /* Start and disable SDK. */
        start();
        Distribute.setEnabled(false).get();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Switch track. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Verify it's persisted. */
        Distribute.unsetInstance();
        start();
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());

        /* Verify no check for update call (or browser). */
        verifyNoMoreInteractions(mHttpClient);
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void persistValueWhenSwitchBeforeStart() {

        /* Switch track before start. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Start. */
        start();

        /* Restart. */
        Distribute.unsetInstance();
        start();

        /* Check. */
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
    }

    @Test
    public void switchTrackBeforeCallCompletes() {

        /* Start (in public mode). */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Check http call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If we switch before flow is completed, it has no effect. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void switchTrackAfterCallCompletes() throws PackageManager.NameNotFoundException {

        /* Start (in public mode). */
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Without browser because its public. */
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* If we switch to private, browser must open. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), eq(mActivity));
    }

    @Test
    public void switchTrackBeforeForeground() throws PackageManager.NameNotFoundException {

        /* Start (in public mode) in background. */
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        start();

        /* Switch to private. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Go foreground. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify browser is opened. */
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), eq(mActivity));
    }

    @Test
    public void switchToSameTrackDoesNothing() {

        /* Start (in public mode). */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* If we switch to public from public, no action. */
        Distribute.setUpdateTrack(UpdateTrack.PUBLIC);
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }
}
