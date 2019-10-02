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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({DistributeUtils.class, SharedPreferencesManager.class, ReleaseDetails.class})
public class DistributeUtilsTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(ReleaseDetails.class);
    }

    @Test
    public void init() {
        new DistributeUtils();
        new DistributeConstants();
    }

    @Test
    public void getNotificationId() {

        /* Coverage fix. */
        int notificationId = DistributeUtils.getNotificationId();
        Assert.assertNotEquals(0, notificationId);
    }

    @Test
    public void loadCachedReleaseDetails() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "release_notes_url: 'https://mock/'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "size: 4242," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails mock = mock(ReleaseDetails.class);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS)).thenReturn(json);
        when(ReleaseDetails.parse(anyString())).thenReturn(mock);

        /* Load. */
        ReleaseDetails releaseDetails = DistributeUtils.loadCachedReleaseDetails();

        /* Verify. */
        assertEquals(mock, releaseDetails);
        verifyStatic(never());
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
        verifyStatic(never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }

    @Test
    public void loadCachedReleaseDetailsJsonException() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "release_notes_url: 'https://mock/'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "size: 4242," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";

        when(SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS)).thenReturn(json);
        when(ReleaseDetails.parse(anyString())).thenThrow(new JSONException("test"));

        /* Load. */
        ReleaseDetails releaseDetails = DistributeUtils.loadCachedReleaseDetails();

        /* Verify. */
        assertNull(releaseDetails);
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }
}
