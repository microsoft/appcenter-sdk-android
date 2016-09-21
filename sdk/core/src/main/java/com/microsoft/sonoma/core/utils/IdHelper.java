package com.microsoft.sonoma.core.utils;

import android.support.annotation.NonNull;

import com.microsoft.sonoma.core.Sonoma;

import java.util.UUID;

import static com.microsoft.sonoma.core.utils.PrefStorageConstants.KEY_INSTALL_ID;

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
            SonomaLog.warn(Sonoma.LOG_TAG, "Unable to get installID from Shared Preferences");
            installId = UUIDUtils.randomUUID();
            StorageHelper.PreferencesStorage.putString(KEY_INSTALL_ID, installId.toString());
        }
        return installId;
    }
}
