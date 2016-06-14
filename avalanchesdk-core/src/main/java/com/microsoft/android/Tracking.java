package com.microsoft.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * <h3>Description</h3>
 *
 * This class defines methods to track the app's usage time.
 *
 **/
public class Tracking {
    /**
     * Key for shared preferences to store a start time.
     */
    protected static final String START_TIME_KEY = "startTime";

    /**
     * Key for shared preferences to store a usage time.
     */
    protected static final String USAGE_TIME_KEY = "usageTime";

    /**
     * Starts tracking of the usage time for the given activity. The current
     * time is saved in shared preferences with a unique key.
     *
     * @param activity Instance of activity
     */
    public static void startUsage(Activity activity) {
        long now = System.currentTimeMillis();

        if (activity == null) {
            return;
        }

        SharedPreferences.Editor editor = getPreferences(activity).edit();
        editor.putLong(START_TIME_KEY + activity.hashCode(), now);
        editor.apply();
    }

    /**
     * Stops tracking of the usage time for the given activity. Reads the
     * start time which was stored by startUsage and calculates the
     * difference. This difference is then added to the total usage time
     * for the current version.
     *
     * @param activity Instance of activity
     */
    public static void stopUsage(Activity activity) {
        long now = System.currentTimeMillis();

        if (activity == null) {
            return;
        }

        if (!checkVersion(activity)) {
            return;
        }

        SharedPreferences preferences = getPreferences(activity);
        long start = preferences.getLong(START_TIME_KEY + activity.hashCode(), 0);
        long sum = preferences.getLong(USAGE_TIME_KEY + Constants.APP_VERSION, 0);

        if (start > 0) {
            long duration = now - start;
            long newSum = sum + duration;

            if (duration <= 0 || newSum < 0) {
                // Don't add negative values or values which cause overflow to tracking
                return;
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(USAGE_TIME_KEY + Constants.APP_VERSION, newSum);
            editor.apply();
        }
    }

    /**
     * Returns the usage time of the current version in seconds.
     *
     * @param context Context to access shared preference.
     * @return Usage time in seconds.
     */
    public static long getUsageTime(Context context) {
        if (!checkVersion(context)) {
            return 0;
        }

        SharedPreferences preferences = getPreferences(context);
        long sum = preferences.getLong(USAGE_TIME_KEY + Constants.APP_VERSION, 0);
        if (sum < 0) {
            preferences.edit().remove(USAGE_TIME_KEY + Constants.APP_VERSION).apply();
            return 0;
        }
        return sum / 1000;
    }

    /**
     * Checks if the versionCode was set. If not, try to load it. Returns false
     * if it is still null.
     */
    private static boolean checkVersion(Context context) {
        if (Constants.APP_VERSION == null) {
            Constants.loadFromContext(context);

            if (Constants.APP_VERSION == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method to return an instance of SharedPreferences.
     *
     * @param context Context to access shared preference.
     * @return Shared preferences instance
     */
    protected static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("Avalanche", Context.MODE_PRIVATE);
    }
}
