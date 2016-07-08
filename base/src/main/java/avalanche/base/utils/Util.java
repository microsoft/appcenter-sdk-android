package avalanche.base.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static final String APP_IDENTIFIER_PATTERN = "[0-9a-f]+";
    public static final int APP_IDENTIFIER_LENGTH = 32;
    private static final Pattern appIdentifierPattern = Pattern.compile(APP_IDENTIFIER_PATTERN, Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes an app identifier or throws an exception if it can't be sanitized.
     * The app identifier must not be null and has to be {@link Util#APP_IDENTIFIER_LENGTH} characters long.
     * It has to match the regular expression pattern {@link Util#APP_IDENTIFIER_PATTERN}.
     *
     * @param appIdentifier the app identifier to sanitize
     * @return the sanitized app identifier
     * @throws IllegalArgumentException if the app identifier can't be sanitized because of unrecoverable input character errors
     */
    public static String sanitizeAppIdentifier(String appIdentifier) throws IllegalArgumentException {

        if (appIdentifier == null) {
            throw new IllegalArgumentException("App ID must not be null.");
        }

        String sAppIdentifier = appIdentifier.trim();

        Matcher matcher = appIdentifierPattern.matcher(sAppIdentifier);

        if (sAppIdentifier.length() != APP_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("App ID length must be " + APP_IDENTIFIER_LENGTH + " characters.");
        } else if (!matcher.matches()) {
            throw new IllegalArgumentException("App ID must match regex pattern /" + APP_IDENTIFIER_PATTERN + "/i");
        }

        return sAppIdentifier;
    }

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
