package com.microsoft.appcenter;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Utility to store and retrieve values for user identifiers.
 */
public class UserIdContext {

    private static final String CUSTOM_PREFIX = "c";

    private static final String PREFIX_SEPARATOR = ":";

    public static boolean checkUserId(String userId) {
        if (userId != null) {
            int prefixIndex = userId.indexOf(PREFIX_SEPARATOR);
            if (prefixIndex == userId.length() - 1) {
                AppCenterLog.error(LOG_TAG, "userId must not be empty.");
                return false;
            }
            if (prefixIndex > 0) {
                String prefix = userId.substring(0, prefixIndex);
                if (!prefix.equals(CUSTOM_PREFIX)) {
                    AppCenterLog.error(LOG_TAG, String.format("userId prefix must be '%s%s', '%s%s:' is not supported.", CUSTOM_PREFIX, PREFIX_SEPARATOR, prefix, PREFIX_SEPARATOR));
                    return false;
                }
            }
        }
        return true;
    }

    public static String getPrefixedUserId(String userId) {
        if (userId != null && !userId.contains(PREFIX_SEPARATOR)) {
            return CUSTOM_PREFIX + PREFIX_SEPARATOR + userId;
        }
        return userId;
    }
}
