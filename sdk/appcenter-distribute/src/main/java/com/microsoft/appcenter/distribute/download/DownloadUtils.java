package com.microsoft.appcenter.distribute.download;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

class DownloadUtils {

    /**
     * Distribute service name.
     */
    static final String SERVICE_NAME = "Distribute";

    /**
     * Base key for stored preferences.
     */
    static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

    /**
     * Preference key to store latest downloaded release hash.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH = PREFERENCE_PREFIX + "downloaded_release_hash";

    /**
     * Preference key to store latest downloaded release id.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_RELEASE_ID = PREFERENCE_PREFIX + "downloaded_release_id";

    /**
     * Preference key to store distribution group identifier of latest downloaded release.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID = PREFERENCE_PREFIX + "downloaded_distribution_group_id";

    /**
     * Get download identifier from storage.
     *
     * @return download identifier or negative value if not found.
     */
    static long getStoredDownloadId() {
        return SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
    }

    /**
     * Get the intent used to open installation U.I.
     *
     * @param fileUri downloaded file URI from the download manager.
     * @return intent to open installation U.I.
     */
    @NonNull
    static Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}