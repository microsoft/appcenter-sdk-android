package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.ArrayList;
import java.util.List;

public final class Utils {

    private static final Gson sGson = new Gson();

    private static final JsonParser sParser = new JsonParser();

    static <T> Document<T> parseDocument(String cosmosDbPayload, Class<T> documentType) {
        return parseDocument(sParser.parse(cosmosDbPayload).getAsJsonObject(), documentType);
    }

    private static <T> Document<T> parseDocument(JsonObject obj, Class<T> documentType) {
        T document = sGson.fromJson(obj.get(Constants.DOCUMENT_FIELD_NAME), documentType);

        return new Document<T>(
                document,
                obj.get(Constants.PARTITION_KEY_FIELD_NAME).getAsString(),
                obj.get(Constants.ID_FIELD_NAME).getAsString(),
                obj.has(Constants.ETAG_FIELD_NAME) ? obj.get(Constants.ETAG_FIELD_NAME).getAsString() : "",
                obj.has(Constants.TIMESTAMP_FIELD_NAME) ? obj.get(Constants.TIMESTAMP_FIELD_NAME).getAsLong() : 0);
    }

    @SuppressWarnings("SameParameterValue")
    static <T> T fromJson(String doc, Class<T> type) {
        return sGson.fromJson(doc, type);
    }

    public static <T> Page<T> parseDocuments(String cosmosDbPayload, Class<T> documentType) {
        JsonObject objects = sParser.parse(cosmosDbPayload).getAsJsonObject();
        JsonArray array = objects.get(Constants.DOCUMENTS_FILED_NAME).getAsJsonArray();
        List<Document<T>> documents = new ArrayList<>();
        for (JsonElement object : array) {
            documents.add(parseDocument(object.getAsJsonObject(), documentType));
        }
        return new Page<T>().withDocuments(documents);
    }

    /**
     * Handle API call failure.
     *
     * @param e Exception to display in the log.
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

    public static Gson getGson() {
        return sGson;
    }
}
