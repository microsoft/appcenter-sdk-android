/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.Constants.READONLY_TABLE;
import static com.microsoft.appcenter.Constants.USER_TABLE_FORMAT;
import static com.microsoft.appcenter.storage.Constants.ETAG_FIELD_NAME;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.USER;

public class Utils {

    private static final Gson sGson = new Gson();

    private static final JsonParser sParser = new JsonParser();

    static <T> Document<T> parseDocument(String cosmosDbPayload, Class<T> documentType) {
        JsonObject body;
        try {
            body = sParser.parse(cosmosDbPayload).getAsJsonObject();
        } catch (RuntimeException e) {
            return new Document<>(e);
        }
        return parseDocument(body, documentType);
    }

    static String getEtag(String cosmosDbPayload) {
        if (cosmosDbPayload == null) {
            return null;
        }
        JsonElement parsedPayload = sParser.parse(cosmosDbPayload);
        if (!parsedPayload.isJsonObject()) {
            return null;
        }
        JsonObject cosmosResponseJson = parsedPayload.getAsJsonObject();
        return cosmosResponseJson.has(ETAG_FIELD_NAME) ?
                cosmosResponseJson.get(ETAG_FIELD_NAME).getAsString() : null;
    }

    private static <T> Document<T> parseDocument(JsonObject obj, Class<T> documentType) {
        try {
            T document = sGson.fromJson(obj.get(Constants.DOCUMENT_FIELD_NAME), documentType);
            return new Document<>(
                    document,
                    obj.get(Constants.PARTITION_KEY_FIELD_NAME).getAsString(),
                    obj.get(Constants.ID_FIELD_NAME).getAsString(),
                    obj.has(ETAG_FIELD_NAME) ? obj.get(ETAG_FIELD_NAME).getAsString() : "",
                    obj.get(Constants.TIMESTAMP_FIELD_NAME).getAsLong());
        } catch (RuntimeException exception) {
            return new Document<>(new StorageException("Failed to deserialize document.", exception));
        }
    }

    @SuppressWarnings("SameParameterValue")
    static <T> T fromJson(String doc, Class<T> type) {
        return sGson.fromJson(doc, type);
    }

    public static <T> Page<T> parseDocuments(String cosmosDbPayload, Class<T> documentType) {
        JsonArray array;
        try {
            JsonObject objects = sParser.parse(cosmosDbPayload).getAsJsonObject();
            array = objects.get(Constants.DOCUMENTS_FIELD_NAME).getAsJsonArray();
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to deserialize Page.", e);
            return new Page<>(new StorageException("Failed to deserialize Page.", e));
        }
        List<Document<T>> documents = new ArrayList<>();
        for (JsonElement object : array) {
            documents.add(parseDocument(object.getAsJsonObject(), documentType));
        }
        return new Page<T>().withDocuments(documents);
    }

    /**
     * Log the exception from a failed API call.
     *
     * @param e Exception to display in the log.
     */
    public static synchronized void logApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(LOG_TAG, "Exception", httpException);
            }
        }
    }

    static String removeAccountIdFromPartitionName(String partition) {
        if (partition.equals(Constants.READONLY)) {
            return partition;
        }
        return partition.substring(0, partition.length() - Constants.PARTITION_KEY_SUFFIX_LENGTH);
    }

    public static Gson getGson() {
        return sGson;
    }

    @NonNull
    static String getTableName(String partition, String accountId) {
        if (USER.equals(partition)) {
            return getUserTableName(accountId);
        }
        return READONLY_TABLE;
    }

    static String getUserTableName() {
        String accountId = AuthTokenContext.getInstance().getAccountId();
        return accountId == null ? null : getUserTableName(accountId);
    }

    @NonNull
    static String getTableName(@NonNull TokenResult tokenResult) {
        if (tokenResult.partition().startsWith(Constants.USER)) {
            return getUserTableName(tokenResult.accountId());
        }
        return READONLY_TABLE;
    }

    static String getUserTableName(String accountId) {
        return String.format(USER_TABLE_FORMAT, accountId).replace("-", "");
    }
}
