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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());

        /* Check changes. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
        Distribute.setUpdateTrack(UpdateTrack.PUBLIC);
        assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());
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
        assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
    }

    @Test
    public void switchTrack() {

        /* Start (in public mode). */
        withoutTesterApp();
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Without browser because its public. */
        verifyStatic(BrowserUtils.class, never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        HttpResponse response = mock(HttpResponse.class);
        PowerMockito.when(response.getPayload()).thenReturn("<mock_release_details>");
        httpCallback.getValue().onCallSucceeded(response);

        /* If we switch to private, browser must not open. We disallow changes. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        verifyStatic(BrowserUtils.class, never());
        BrowserUtils.openBrowser(anyString(), eq(mActivity));
    }

    @Test
    public void switchTrackBeforeForeground() {

        /* Start (in public mode) in background. */
        withoutTesterApp();
        start();

        /* Switch to private. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Check value didn't change. */
        assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());

        /* Go foreground. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify browser is not opened. Changes are allowed only before start. */
        verifyStatic(BrowserUtils.class, never());
        BrowserUtils.openBrowser(anyString(), eq(mActivity));
    }
}
