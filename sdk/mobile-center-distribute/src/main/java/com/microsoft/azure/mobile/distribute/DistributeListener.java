package com.microsoft.azure.mobile.distribute;

import android.app.Activity;
import android.support.annotation.UiThread;

/**
 * Listener for the Distribute allowing customization.
 */
@SuppressWarnings("unused")
public interface DistributeListener {

    /**
     * Called whenever a new release is available to download.
     * <p>
     * If user does not action the release (either postpone, ignore or download), this callback
     * will repeat for every activity change for the same release.
     * <p>
     * If you are showing your own U.I. for the new release, return <code>true</code> to this method
     * and when call {@link Distribute#notifyUserUpdateAction(int)} when the user action the U.I.
     *
     * @param activity       current activity.
     * @param releaseDetails release details for the update.
     * @return the custom dialog whose visibility will be managed for you if not null.
     */
    @UiThread
    boolean onNewReleaseAvailable(Activity activity, ReleaseDetails releaseDetails);
}
