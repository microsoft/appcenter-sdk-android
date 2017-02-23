package com.microsoft.azure.mobile.updates;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Release details JSON schema.
 */
class ReleaseDetails {

    private static final String ID = "id";

    private static final String VERSION = "version";

    private static final String SHORT_VERSION = "short_version";

    private static final String RELEASE_NOTES = "release_notes";

    private static final String DOWNLOAD_URL = "download_url";

    /**
     * ID identifying this unique release.
     */
    private int id;

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
        releaseDetails.id = object.getInt(ID);
        try {
            releaseDetails.version = Integer.parseInt(object.getString(VERSION));
        } catch (NumberFormatException e) {
            throw new JSONException(e.getMessage());
        }
        releaseDetails.shortVersion = object.getString(SHORT_VERSION);
        releaseDetails.releaseNotes = object.isNull(RELEASE_NOTES) ? null : object.getString(RELEASE_NOTES);
        releaseDetails.downloadUrl = Uri.parse(object.getString(DOWNLOAD_URL));
        String scheme = releaseDetails.downloadUrl.getScheme();
        if (scheme == null || !scheme.startsWith("http")) {
            throw new JSONException("Invalid download_url scheme.");
        }
        return releaseDetails;
    }

    /**
     * Get the id value.
     *
     * @return the id value.
     */
    int getId() {
        return id;
    }

    /**
     * Get the version value.
     *
     * @return the version value
     */
    int getVersion() {
        return version;
    }

    /**
     * Get the shortVersion value.
     *
     * @return the shortVersion value
     */
    @NonNull
    String getShortVersion() {
        return shortVersion;
    }

    /**
     * Get the releaseNotes value.
     *
     * @return the releaseNotes value
     */
    @Nullable
    String getReleaseNotes() {
        return releaseNotes;
    }

    /**
     * Get the downloadUrl value.
     *
     * @return the downloadUrl value
     */
    @NonNull
    Uri getDownloadUrl() {
        return downloadUrl;
    }
}
