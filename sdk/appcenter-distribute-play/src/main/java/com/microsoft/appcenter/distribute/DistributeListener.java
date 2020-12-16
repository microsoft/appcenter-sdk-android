/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;

/**
 * Listener for the Distribute allowing customization.
 */
@SuppressWarnings("unused")
public interface DistributeListener {

    /**
     * Called from UI thread whenever a new release is available to download and install.
     * <p>
     * If user does not action the release (either postpone or update), this callback
     * will repeat for every activity change for the same release.
     * <p>
     * If you are showing your own U.I. for the new release, return <code>true</code> to this method
     * and when call {@link Distribute#notifyUpdateAction(int)} when the user action the U.I.
     *
     * @param activity       current activity.
     * @param releaseDetails release details for the update.
     * @return the custom dialog whose visibility will be managed for you if not null.
     */
    boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails);

    /**
     * Called when no updates are available for the application.
     * <p>
     * This callback will repeat for every foreground/background state change if there are no updates.
     * <p>
     * It's easier to use Toast or Snackbar here, but if you prefer a dialog and you use custom
     * dialogs for {@link DistributeListener#onReleaseAvailable(Activity, ReleaseDetails)},
     * make sure to dismiss the previous onReleaseAvailable dialog if it is still showing.
     *
     * @param activity current activity.
     */
    boolean onNoReleaseAvailable(Activity activity);
}
