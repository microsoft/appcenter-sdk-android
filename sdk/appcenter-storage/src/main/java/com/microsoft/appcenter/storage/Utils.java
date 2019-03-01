package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.utils.AppCenterLog;

public final class Utils {

    public static final Gson sGson = new Gson();

    public static synchronized <T> Document<T> parseDocument(String documentPayload) {
        return Utils.sGson.fromJson(documentPayload, new TypeToken<Document<T>>() {
        }.getType());
    }

    public static synchronized <T> Page<T> parseDocuments(String documentPayload) {
        return Utils.sGson.fromJson(documentPayload, new TypeToken<Page<T>>(){
        }.getType());
    }

    /**
     * Handle API call failure.
     *
     * @param e Exception to display in the log
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
