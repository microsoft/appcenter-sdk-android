package com.microsoft.appcenter.utils;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Test;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.USER_ID_APP_CENTER_MAX_LENGTH;

/**
 * Utility to store and retrieve values for user identifiers.
 */
public class UserIdUtils {

    private static final String CUSTOM_PREFIX = "c";

    private static final String PREFIX_SEPARATOR = ":";

    /**
     * Check if userId is valid for One Collector.
     *
     * @param userId userId.
     * @return true if valid, false otherwise.
     */
    public static boolean checkUserIdValidForOneCollector(String userId) {
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
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Check if userId is valid for App Center.
     *
     * @param userId userId.
     * @return true if valid, false otherwise.
     */
    public static boolean checkUserIdValidForAppCenter(String userId) {
        if (userId.length() > USER_ID_APP_CENTER_MAX_LENGTH) {
            AppCenterLog.error(LOG_TAG, "userId is limited to " + USER_ID_APP_CENTER_MAX_LENGTH + " characters.");
            return false;
        }
        return true;
    }

    /**
     * Add 'c:' prefix to userId if the userId has no prefix.
     *
     * @param userId userId.
     * @return prefixed userId or null if the userId was null.
     */
    public static String getPrefixedUserId(String userId) {
        if (userId != null && !userId.contains(PREFIX_SEPARATOR)) {
            return CUSTOM_PREFIX + PREFIX_SEPARATOR + userId;
        }
        return userId;
    }
}
