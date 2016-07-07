package avalanche.base.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Util {

    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    public static String getAppName(Context context) {
        if (context == null) {
            return "";
        }

        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            AvalancheLog.error("Could not find self package info", e);
        }
        return (applicationInfo != null ? (String) packageManager.getApplicationLabel(applicationInfo)
                : context.getString(0));
    }

    /**
     * Returns a string created by each element of the array, separated by
     * delimiter.
     */
    public static String joinArray(String[] array, String delimiter) {
        StringBuilder buffer = new StringBuilder();
        for (int index = 0; index < array.length; index++) {
            buffer.append(array[index]);
            if (index < array.length - 1) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static boolean isMainActivity(Activity activity) {
        return activity.getIntent().getAction().equals(Intent.ACTION_MAIN);
    }
}
