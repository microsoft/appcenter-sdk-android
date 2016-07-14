package avalanche.core.utils;

import java.util.UUID;

import static avalanche.core.utils.PrefStorageConstants.KEY_INSTALL_ID;

public class IdHelper {
    /**
     * Get the installID from the Shared preferences. In case this fails, will generate a new installId.
     * @return the installID
     */
    public static UUID getInstallId() {
        String installIdString = StorageHelper.PreferencesStorage.getString(KEY_INSTALL_ID, "");
        UUID installId;
        try {
            installId = UUID.fromString(installIdString);
        }
        catch (Exception e) {
            AvalancheLog.warn("Unable to get installID from Shared Preferences");
            installId = UUID.randomUUID();
            StorageHelper.PreferencesStorage.putString(KEY_INSTALL_ID, installId.toString());
        }
        return installId;
    }
}
