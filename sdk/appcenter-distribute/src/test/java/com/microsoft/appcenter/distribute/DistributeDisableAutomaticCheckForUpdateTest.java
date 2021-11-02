/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class DistributeDisableAutomaticCheckForUpdateTest extends AbstractDistributeTest {

    @Test
    public void disableAutomaticCheckForUpdateBeforeDistributeStartDoesNotCheckForUpdate() {

        /* Disable automatic check for update then start. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* No HTTP call done. */
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void disableAutomaticCheckForUpdateAfterDistributeStartChecksForUpdate() {

        /* Start then call disable automatic check for update after Distribute has started. */
        start();
        Distribute.disableAutomaticCheckForUpdate();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void manualCheckWhileAutomaticCheckDisabledChecksForUpdate() {

        /* Start then call disable automatic check for update after Distribute has started. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.checkForUpdate();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void manualCheckAfterActivityResumedWhileAutomaticCheckDisabledChecksForUpdate() {

        /* Start then call disable automatic check for update after Distribute has started. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.checkForUpdate();

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkForUpdateAfterManualCheckCompletesDoesNotCheckForUpdatesAgain() {

        /* Start in public mode with automatic check for updates disabled. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.checkForUpdate();

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* Restart. */
        restartResumeLauncher(mActivity);

        /* Do not call again. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));

        /* Check for update manually again and verify one more call. */
        Distribute.checkForUpdate();
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));
    }

    @Test
    public void manualCheckForUpdateAfterManualCheckCompletesChecksForUpdateAgain() {

        /* Start in public mode with automatic check for updates disabled. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.checkForUpdate();

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* Manually check for updates again. */
        Distribute.checkForUpdate();

        /* Http call done again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));
    }
}
