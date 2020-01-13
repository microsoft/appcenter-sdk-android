/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.UUID;

import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_INSTALL_ID;

public class IdHelper {

    /**
     * Get the installID from the Shared preferences. In case this fails, will generate a new installId.
     *
     * @return the installID
     */
    @NonNull
    public static UUID getInstallId() {
        String installIdString = SharedPreferencesManager.getString(KEY_INSTALL_ID, "");
        UUID installId;
        try {
            installId = UUID.fromString(installIdString);
        } catch (Exception e) {
            AppCenterLog.warn(AppCenter.LOG_TAG, "Unable to get installID from Shared Preferences");
            installId = UUID.randomUUID();
            SharedPreferencesManager.putString(KEY_INSTALL_ID, installId.toString());
        }
        return installId;
    }
}
