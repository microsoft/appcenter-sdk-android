package avalanche.base.utils;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.utils.StorageHelper.PreferencesStorage.SHARED_PREFS_APP_ID;
import static avalanche.base.utils.StorageHelper.PreferencesStorage.SHARED_PREFS_INSTALL_ID;


@Retention(RetentionPolicy.SOURCE)
@StringDef({
        SHARED_PREFS_INSTALL_ID,
        SHARED_PREFS_APP_ID
})
public @interface StorageHelperDef {
}