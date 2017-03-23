package com.microsoft.azure.mobile.distribute;

import android.content.DialogInterface;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Constants to use for {@link Distribute#handleUserUpdateAction}.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        UserUpdateAction.DOWNLOAD,
        UserUpdateAction.IGNORE,
        UserUpdateAction.POSTPONE
})
public @interface UserUpdateAction {

    /**
     * Action to trigger the download of the release.
     */
    int DOWNLOAD = DialogInterface.BUTTON_POSITIVE;

    /**
     * Action to ignore this particular release. Only a new release will trigger an update dialog.
     */
    int IGNORE = DialogInterface.BUTTON_NEGATIVE;

    /**
     * Action to postpone the update to next application restart.
     */
    int POSTPONE = DialogInterface.BUTTON_NEUTRAL;
}
