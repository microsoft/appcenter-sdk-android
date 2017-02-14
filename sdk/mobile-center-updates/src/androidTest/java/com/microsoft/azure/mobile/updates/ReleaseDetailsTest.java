package com.microsoft.azure.mobile.updates;

import android.net.Uri;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ReleaseDetailsTest {

    @Test
    public void parse() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertEquals("Fix a critical bug, this text was entered in Mobile Center portal.", releaseDetails.getReleaseNotes());
        assertEquals("", releaseDetails.getMinOs());
        assertEquals("b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c", releaseDetails.getFingerprint());
        assertEquals(Uri.parse("http://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
    }

    @Test(expected = JSONException.class)
    public void missingVersion() throws JSONException {
        String json = "{" +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidVersion() throws JSONException {
        String json = "{" +
                "version: true," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingShortVersion() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test
    public void missingReleaseNotes() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'https://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertNull(releaseDetails.getReleaseNotes());
        assertEquals("", releaseDetails.getMinOs());
        assertEquals("b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c", releaseDetails.getFingerprint());
        assertEquals(Uri.parse("https://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
    }

    @Test
    public void nullReleaseNotes() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "release_notes: null," +
                "short_version: '2.1.5'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'https://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails releaseDetails = ReleaseDetails.parse(json);
        assertNotNull(releaseDetails);
        assertEquals(14, releaseDetails.getVersion());
        assertEquals("2.1.5", releaseDetails.getShortVersion());
        assertNull(releaseDetails.getReleaseNotes());
        assertEquals("", releaseDetails.getMinOs());
        assertEquals("b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c", releaseDetails.getFingerprint());
        assertEquals(Uri.parse("https://download.thinkbroadband.com/1GB.zip"), releaseDetails.getDownloadUrl());
    }

    @Test(expected = JSONException.class)
    public void missingMinOs() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingFingerprint() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "download_url: 'http://download.thinkbroadband.com/1GB.zip'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingDownloadUrl() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void missingDownloadUrlScheme() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'someFile'" +
                "}";
        ReleaseDetails.parse(json);
    }

    @Test(expected = JSONException.class)
    public void invalidDownloadUrlScheme() throws JSONException {
        String json = "{" +
                "version: '14'," +
                "short_version: '2.1.5'," +
                "release_notes: 'Fix a critical bug, this text was entered in Mobile Center portal.'," +
                "min_os: ''," +
                "fingerprint: 'b407a9acbbdf509de2af3676de8d8fa26a21e4293a393dcef7d902deaa9caa1c'," +
                "download_url: 'ftp://someFile'" +
                "}";
        ReleaseDetails.parse(json);
    }
}
