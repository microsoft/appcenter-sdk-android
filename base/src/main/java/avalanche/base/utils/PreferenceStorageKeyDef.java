package avalanche.base.utils;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.utils.PrefStorageConstants.KEY_APP_KEY;
import static avalanche.base.utils.PrefStorageConstants.KEY_INSTALL_ID;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        KEY_INSTALL_ID,
        KEY_APP_KEY
})
public @interface PreferenceStorageKeyDef {
}