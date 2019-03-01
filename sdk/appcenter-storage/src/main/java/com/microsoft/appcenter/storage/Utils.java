package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.utils.AppCenterLog;

public final class Utils {

    public static final Gson sGson = new Gson();

    public static synchronized <T> Document<T> parseDocument(String cosmosDbPayload, Class<T> documentType) {
        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(cosmosDbPayload).getAsJsonObject();
        T document = sGson.fromJson(obj.get("document"), documentType);
        return new Document<T>(
                document,
                obj.get("PartitionKey").getAsString(),
                obj.get("id").getAsString(),
                obj.get("_etag").getAsString(),
                obj.get("_ts").getAsLong());
    }

    public static synchronized <T> T fromJson(String doc, Class<T> type) {
        return sGson.fromJson(doc, type);
    }

    /**
     * Handle API call failure.
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
