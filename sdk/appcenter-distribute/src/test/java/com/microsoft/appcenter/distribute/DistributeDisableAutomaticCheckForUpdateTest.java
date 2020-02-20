/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import java.util.Collections;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        verify(mHttpClient, times(0)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.checkForUpdate();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void storeRedirectionParametersDoesNotCheckForUpdateWhenAutomaticCheckDisabled() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Enable in-app updates. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* No HTTP call done. */
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void storeRedirectionParametersCheckForUpdateWhenAutomaticCheckNotDisabled() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        
        /* Enable in-app updates. */
        start();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void manualCheckWhenStoringRedirectionParametersWhileAutomaticCheckDisabledChecksForUpdate() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Start then call disable automatic check for update after Distribute has started. */
        Distribute.disableAutomaticCheckForUpdate();
        start();
        verify(mHttpClient, times(0)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.checkForUpdate();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* HTTP call done. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }
}
