/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({
        DistributeUtils.class,
        SharedPreferencesManager.class,
        ReleaseDetails.class
})
public class DistributeUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(ReleaseDetails.class);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void init() {
        new DistributeUtils();
        new DistributeConstants();
    }

    @Test
    public void loadCachedReleaseDetails() throws JSONException {
        ReleaseDetails mock = mock(ReleaseDetails.class);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS)).thenReturn("test");
        when(ReleaseDetails.parse(anyString())).thenReturn(mock);

        /* Load. */
        ReleaseDetails releaseDetails = DistributeUtils.loadCachedReleaseDetails();

        /* Verify. */
        assertEquals(mock, releaseDetails);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }

    @Test
    public void loadCachedReleaseDetailsNull() {
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS)).thenReturn(null);

        /* Load. */
        ReleaseDetails releaseDetails = DistributeUtils.loadCachedReleaseDetails();

        /* Verify. */
        assertNull(releaseDetails);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }

    @Test
    public void loadCachedReleaseDetailsJsonException() throws JSONException {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS)).thenReturn("test");
        when(ReleaseDetails.parse(anyString())).thenThrow(new JSONException("test"));

        /* Load. */
        ReleaseDetails releaseDetails = DistributeUtils.loadCachedReleaseDetails();

        /* Verify. */
        assertNull(releaseDetails);
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }
}
