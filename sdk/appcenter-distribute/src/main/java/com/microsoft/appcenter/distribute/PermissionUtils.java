/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Permission utils.
 */
public class PermissionUtils {

    /**
     * Fills an array with app's permissions' states regarding passed permissions array.
     *
     * @param context     Context
     * @param permissions an array with specified permissions
     * @return an array with either {@link PackageManager#PERMISSION_GRANTED} if the calling
     * pid/uid is allowed that permission, or
     * {@link PackageManager#PERMISSION_DENIED} if it is not.
     */
    public static int[] permissionsState(Context context, String... permissions) {
        if (permissions == null) {
            return null;
        }
        int[] state = new int[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            state[i] = context.checkCallingOrSelfPermission(permissions[i]);
        }
        return state;
    }

    /**
     * Checks if the specified permissions' states are equal to {@link PackageManager#PERMISSION_GRANTED}.
     *
     * @param permissionsState an array with permissions' states.
     * @return true if granted, false otherwise.
     */
    public static boolean permissionsAreGranted(int[] permissionsState) {
        for (int permissionState : permissionsState) {
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

