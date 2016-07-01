package avalanche.base.utils;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.utils.PrefStorageConstants.KEY_APP_ID;
import static avalanche.base.utils.PrefStorageConstants.KEY_INSTALL_ID;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        KEY_INSTALL_ID,
        KEY_APP_ID
})
public @interface PreferenceStorageKeyDef {
}