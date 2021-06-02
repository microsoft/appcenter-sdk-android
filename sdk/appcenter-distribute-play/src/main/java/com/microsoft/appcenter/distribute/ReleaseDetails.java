/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
/**
 * Release details JSON schema.
 */
@SuppressWarnings("WeakerAccess")
public class ReleaseDetails {

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public int getId() {
        return 1;
    }

    /**
     * Get the version value.
     *
     * @return the version value
     */
    public int getVersion() {
        return 1;
    }

    /**
     * Get the release's package size.
     *
     * @return the release's package size.
     */
    public long getSize() {
        return 0;
    }

    /**
     * Get the shortVersion value.
     *
     * @return the shortVersion value
     */
    @NonNull
    public String getShortVersion() {
        return "shortVersion";
    }

    /**
     * Get the releaseNotes value.
     *
     * @return the releaseNotes value
     */
    @Nullable
    public String getReleaseNotes() {
        return "releaseNotes";
    }

    /**
     * Get the releasesNotesUrl value.
     *
     * @return the releaseNotesUrl value.
     */
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public Uri getReleaseNotesUrl() {
        return null;
    }

    /**
     * Get the minApiLevel value.
     *
     * @return the minApiLevel value
     */
    int getMinApiLevel() {
        return 0;
    }

    /**
     * Get the downloadUrl value.
     *
     * @return the downloadUrl value
     */
    @NonNull
    public Uri getDownloadUrl() {
        return null;
    }

    /**
     * Get the mandatory update value.
     *
     * @return mandatory update value
     */
    public boolean isMandatoryUpdate() {
        return false;
    }

    /**
     * Get the release hash value.
     *
     * @return the releaseHash value
     */
    @NonNull
    public String getReleaseHash() {
        return "releaseHash";
    }

    /**
     * Get the distribution group identifier value.
     *
     * @return the distributionGroupId value.
     */
    public String getDistributionGroupId() {
        return "distributionGroupId";
    }
}
