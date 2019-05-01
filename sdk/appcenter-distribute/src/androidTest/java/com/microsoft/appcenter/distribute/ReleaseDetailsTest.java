/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.net.Uri;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReleaseDetailsTest {

    @Test
    public void parse() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "release_notes_url: 'https://mock/'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in App Center portal.", releaseDetails.getReleaseNotes());
        assertEquals(Uri.parse("https://mock/"), releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
        assertEquals("fd37a4b1-4937-45ef-97fb-b864154371f0", releaseDetails.getDistributionGroupId());
    }

    @Test(expected = JSONException.class)
    public void missingId() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidId() throws JSONException {
        String json = "{" +
                "id: '42abc'," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void acceptIdAsString() throws JSONException {
        String json = "{" +
                "id: '42'," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in App Center portal.", releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }

    @Test(expected = JSONException.class)
    public void missingVersion() throws JSONException {
        String json = "{" +
                "id: 42," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidVersion() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: true," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingShortVersion() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void missingReleaseNotes() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "android_min_api_level: 19," +
                "download_url: 'https://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertNull(releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("https://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }

    @Test
    public void nullReleaseNotes() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "release_notes: null," +
                "android_min_api_level: 19," +
                "short_version: '2.1.5'," +
                "download_url: 'https://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertNull(releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("https://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }


    @Test
    public void nullReleaseNotesUrl() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "release_notes: null," +
                "release_notes_url: null," +
                "android_min_api_level: 19," +
                "short_version: '2.1.5'," +
                "download_url: 'https://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertNull(releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("https://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }

    @Test(expected = JSONException.class)
    public void missingApiLevel() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void acceptApiLevelAsString() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: '19'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in App Center portal.", releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }

    @Test(expected = JSONException.class)
    public void invalidApiLevel() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: '4.0.3'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingDownloadUrl() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19" +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingDownloadUrlScheme() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'someFile'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidDownloadUrlScheme() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'ftp://someFile'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void mandatoryUpdate() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: true," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in App Center portal.", releaseDetails.getReleaseNotes());
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertTrue(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
    }

    @Test(expected = JSONException.class)
    public void missingMandatoryUpdate() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingPackageHashes() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "mandatory_update: false," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void emptyPackageHashes() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: []," +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidPackageHashes() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: '9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60'" +
                "distribution_group_id: 'fd37a4b1-4937-45ef-97fb-b864154371f0'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void nullDistributionGroupId() throws JSONException {
        String json = "{" +
                "id: 42," +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in App Center portal.'," +
                "release_notes_url: 'https://mock/'," +
                "android_min_api_level: 19," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'," +
                "mandatory_update: false," +
                "package_hashes: ['9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60']" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(42, releaseDetails.getId());
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in App Center portal.", releaseDetails.getReleaseNotes());
        assertEquals(Uri.parse("https://mock/"), releaseDetails.getReleaseNotesUrl());
        assertEquals(19, releaseDetails.getMinApiLevel());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
        assertFalse(releaseDetails.isMandatoryUpdate());
        assertEquals("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60", releaseDetails.getReleaseHash());
        assertNull(releaseDetails.getDistributionGroupId());
    }
}
