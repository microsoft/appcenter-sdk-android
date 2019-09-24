// TODO Copyright

package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.pm.PackageManager;

// TODO Rename PermissionsUtils
public class PermissionsUtil {

    // TODO JavaDoc
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

    // TODO JavaDoc
    public static boolean permissionsAreGranted(int[] permissionsState) {
        for (int permissionState : permissionsState) {
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

