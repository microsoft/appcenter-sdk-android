/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Release details JSON schema.
 */
@SuppressWarnings("WeakerAccess")
public class ReleaseDetails {

    private static final String ID = "id";

    private static final String VERSION = "version";

    private static final String SHORT_VERSION = "short_version";

    private static final String RELEASE_NOTES = "release_notes";

    private static final String RELEASE_NOTES_URL = "release_notes_url";

    private static final String MIN_API_LEVEL = "android_min_api_level";

    private static final String DOWNLOAD_URL = "download_url";

    private static final String MANDATORY_UPDATE = "mandatory_update";

    private static final String PACKAGE_HASHES = "package_hashes";

    private static final String DISTRIBUTION_GROUP_ID = "distribution_group_id";

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
     * The release notes URL.
     */
    private Uri releaseNotesUrl;

    /**
     * The release's minimum required Android API level.
     */
    private int minApiLevel;

    /**
     * The URL that hosts the binary for this release.
     */
    private Uri downloadUrl;

    /**
     * Mandatory update.
     */
    private boolean mandatoryUpdate;

    /**
     * Release hash.
     */
    private String releaseHash;

    /**
     * Distribution group identifier.
     */
    private String distributionGroupId;

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
        releaseDetails.version = object.getInt(VERSION);
        releaseDetails.shortVersion = object.getString(SHORT_VERSION);
        releaseDetails.releaseNotes = object.isNull(RELEASE_NOTES) ? null : object.getString(RELEASE_NOTES);
        releaseDetails.releaseNotesUrl = object.isNull(RELEASE_NOTES_URL) ? null : Uri.parse(object.getString(RELEASE_NOTES_URL));
        releaseDetails.minApiLevel = object.getInt(MIN_API_LEVEL);
        releaseDetails.downloadUrl = Uri.parse(object.getString(DOWNLOAD_URL));
        String scheme = releaseDetails.downloadUrl.getScheme();
        if (scheme == null || !scheme.startsWith("http")) {
            throw new JSONException("Invalid download_url scheme.");
        }
        releaseDetails.mandatoryUpdate = object.getBoolean(MANDATORY_UPDATE);
        releaseDetails.releaseHash = object.getJSONArray(PACKAGE_HASHES).getString(0);
        releaseDetails.distributionGroupId = object.isNull(DISTRIBUTION_GROUP_ID) ? null : object.getString(DISTRIBUTION_GROUP_ID);
        return releaseDetails;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public int getId() {
        return id;
    }

    /**
     * Get the version value.
     *
     * @return the version value
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the shortVersion value.
     *
     * @return the shortVersion value
     */
    @NonNull
    public String getShortVersion() {
        return shortVersion;
    }

    /**
     * Get the releaseNotes value.
     *
     * @return the releaseNotes value
     */
    @Nullable
    public String getReleaseNotes() {
        return releaseNotes;
    }

    /**
     * Get the releasesNotesUrl value.
     *
     * @return the releaseNotesUrl value.
     */
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public Uri getReleaseNotesUrl() {
        return releaseNotesUrl;
    }

    /**
     * Get the minApiLevel value.
     *
     * @return the minApiLevel value
     */
    int getMinApiLevel() {
        return minApiLevel;
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

    /**
     * Get the mandatory update value.
     *
     * @return mandatory update value
     */
    public boolean isMandatoryUpdate() {
        return mandatoryUpdate;
    }

    /**
     * Get the release hash value.
     *
     * @return the releaseHash value
     */
    @NonNull
    String getReleaseHash() {
        return releaseHash;
    }

    /**
     * Get the distribution group identifier value.
     *
     * @return the distributionGroupId value.
     */
    String getDistributionGroupId() {
        return distributionGroupId;
    }
}
