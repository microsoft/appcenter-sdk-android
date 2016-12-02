/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.utils;

import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.UUID;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_INSTALL_ID;

public class IdHelper {

    /**
     * Get the installID from the Shared preferences. In case this fails, will generate a new installId.
     *
     * @return the installID
     */
    @NonNull
    public static UUID getInstallId() {
        String installIdString = StorageHelper.PreferencesStorage.getString(KEY_INSTALL_ID, "");
        UUID installId;
        try {
            installId = UUID.fromString(installIdString);
        } catch (Exception e) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Unable to get installID from Shared Preferences");
            installId = UUIDUtils.randomUUID();
            StorageHelper.PreferencesStorage.putString(KEY_INSTALL_ID, installId.toString());
        }
        return installId;
    }
}
