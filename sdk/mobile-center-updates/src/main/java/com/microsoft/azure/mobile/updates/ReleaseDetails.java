package com.microsoft.azure.mobile.updates;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Release details JSON schema.
 */
class ReleaseDetails {

    private static final String VERSION = "version";

    private static final String SHORT_VERSION = "short_version";

    private static final String RELEASE_NOTES = "release_notes";

    private static final String MIN_OS = "min_os";

    private static final String FINGERPRINT = "fingerprint";

    private static final String DOWNLOAD_URL = "download_url";

    /**
     * The release's version.<br>
     * For iOS: CFBundleVersion from info.plist.
     * For Android: android:versionCode from AppManifest.xml.
     */
    private int version;

    /**
     * The release's short version.<br>
     * For iOS: CFBundleShortVersionString from info.plist.
     * For Android: android:versionName from AppManifest.xml.
     */
    private String shortVersion;

    /**
     * The release's release notes.
     */
    private String releaseNotes;

    /**
     * The release's minimum required operating system.
     */
    private String minOs;

    /**
     * Checksum of the release binary.
     */
    private String fingerprint;

    /**
     * The URL that hosts the binary for this release.
     */
    private Uri downloadUrl;

    /**
     * Parse a JSON string describing release details.
     *
     * @param json a string.
     * @return parsed release details.
     * @throws JSONException if JSON is invalid.
     */
    static ReleaseDetails parse(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        ReleaseDetails releaseDetails = new ReleaseDetails();
        try {
            releaseDetails.version = Integer.parseInt(object.getString(VERSION));
        } catch (NumberFormatException e) {
            throw new JSONException(e.getMessage());
        }
        releaseDetails.shortVersion = object.getString(SHORT_VERSION);
        releaseDetails.releaseNotes = object.isNull(RELEASE_NOTES) ? null : object.getString(RELEASE_NOTES);
        releaseDetails.minOs = object.getString(MIN_OS);
        releaseDetails.fingerprint = object.getString(FINGERPRINT);
        releaseDetails.downloadUrl = Uri.parse(object.getString(DOWNLOAD_URL));
        String scheme = releaseDetails.downloadUrl.getScheme();
        if (scheme == null || !scheme.startsWith("http")) {
            throw new JSONException("Invalid download_url scheme.");
        }
        return releaseDetails;
    }

    /**
     * Get the version value.
     *
     * @return the version value
     */
    int getVersion() {
        return this.version;
    }

    /**
     * Get the shortVersion value.
     *
     * @return the shortVersion value
     */
    String getShortVersion() {
        return shortVersion;
    }

    /**
     * Get the releaseNotes value.
     *
     * @return the releaseNotes value
     */
    String getReleaseNotes() {
        return this.releaseNotes;
    }

    /**
     * Get the minOs value.
     *
     * @return the minOs value
     */
    String getMinOs() {
        return this.minOs;
    }

    /**
     * Get the fingerprint value.
     *
     * @return the fingerprint value
     */
    String getFingerprint() {
        return this.fingerprint;
    }

    /**
     * Get the downloadUrl value.
     *
     * @return the downloadUrl value
     */
    Uri getDownloadUrl() {
        return this.downloadUrl;
    }
}
