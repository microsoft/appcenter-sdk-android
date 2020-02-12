/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCallback;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DistributeManualCheckForUpdateTest extends AbstractDistributeTest {

    @Test
    public void checkForUpdateAfterWorkflowCompletesChecksAgain() {

        /* Start in public mode with automatic check for updates. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete call with no new release (this will return the default mock mReleaseDetails with version 0). */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* If checking for updates again. */
        Distribute.checkForUpdate();

        /* Then we call again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));
    }

    @Test
    public void checkForUpdateBeforeCallCompletes() {

        /* Start in public mode with automatic check for updates. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Check http call done. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* If checking for updates again before call completes. */
        Distribute.checkForUpdate();

        /* Then we don't call again. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));

        /* And it's not queued when current call finishes with no update available. */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), notNull(ServiceCallback.class));
    }

    @Test
    public void checkForUpdatesWhenDisabledDoesNotWork() {

        /* Start and disable. */
        start();
        Distribute.setEnabled(false).get();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Check for updates. */
        Distribute.checkForUpdate();

        /* No HTTP call done. */
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }
}
