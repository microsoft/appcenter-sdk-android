package com.microsoft.azure.mobile.distribute;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.UiThread;

/**
 * Listener for the Distribute allowing customization.
 */
@SuppressWarnings("unused")
public interface DistributeListener {

    /**
     * Called whenever an update dialog should be shown or restored.
     *
     * @param releaseDetails release details for the update.
     * @return true to customize update dialog, false to use the default one.
     */
    @UiThread
    boolean shouldCustomizeUpdateDialog(ReleaseDetails releaseDetails);

    /**
     * Called if {@link #shouldCustomizeUpdateDialog} returns true to build custom update dialog.
     * <p>
     * If you want to notify the user with something else than a dialog, just return null.
     *
     * @param activity       current activity.
     * @param releaseDetails release details for the update.
     * @return the custom dialog whose visibility will be managed for you if not null.
     */
    @UiThread
    Dialog buildUpdateDialog(Activity activity, ReleaseDetails releaseDetails);
}
