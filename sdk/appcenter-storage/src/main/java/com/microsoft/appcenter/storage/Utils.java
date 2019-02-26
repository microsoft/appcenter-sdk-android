package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

public final class Utils {
    public static final Gson sGson = new Gson();

    /**
     * Handle API call failure.
     */
    public static synchronized void handleApiCallFailure(Exception e) {
        AppCenterLog.error(Constants.LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(Constants.LOG_TAG, "Exception", httpException);
            }
        }
    }
}
