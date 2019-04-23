/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.appcenter.data.exception.StorageException;
import com.microsoft.appcenter.data.models.Document;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.TokenResult;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.microsoft.appcenter.Constants.READONLY_TABLE;
import static com.microsoft.appcenter.Constants.USER_TABLE_FORMAT;
import static com.microsoft.appcenter.data.Constants.DOCUMENT_FIELD_NAME;
import static com.microsoft.appcenter.data.Constants.ETAG_FIELD_NAME;
import static com.microsoft.appcenter.data.Constants.ID_FIELD_NAME;
import static com.microsoft.appcenter.data.Constants.LOG_TAG;
import static com.microsoft.appcenter.data.Constants.PARTITION_KEY_FIELD_NAME;
import static com.microsoft.appcenter.data.Constants.TIMESTAMP_FIELD_NAME;
import static com.microsoft.appcenter.data.Constants.USER;

public class Utils {

    private static final Gson sGson = new GsonBuilder().registerTypeAdapter(Date.class, new DateAdapter()).create();

    private static final JsonParser sParser = new JsonParser();

    private static class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private final DateFormat mDateFormat;

        DateAdapter() {
            mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public synchronized JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(mDateFormat.format(src));
        }

        @Override
        public synchronized Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return mDateFormat.parse(json.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }

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
            T document = sGson.fromJson(obj.get(DOCUMENT_FIELD_NAME), documentType);
            return new Document<>(
                    document,
                    obj.get(PARTITION_KEY_FIELD_NAME).getAsString(),
                    obj.get(ID_FIELD_NAME).getAsString(),
                    obj.has(ETAG_FIELD_NAME) ? obj.get(ETAG_FIELD_NAME).getAsString() : null,
                    obj.get(TIMESTAMP_FIELD_NAME).getAsLong());
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
        return new Page<T>().setItems(documents);
    }

    /**
     * Log the exception from a failed API call.
     *
     * @param e Exception to display in the log.
     */
    public static synchronized void logApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
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
        if (tokenResult.getPartition().startsWith(Constants.USER)) {
            return getUserTableName(tokenResult.getAccountId());
        }
        return READONLY_TABLE;
    }

    static String getUserTableName(String accountId) {
        return String.format(USER_TABLE_FORMAT, accountId).replace("-", "");
    }

    static String getOutgoingId(String partition, String documentId) {
        return String.format("%s_%s", partition, documentId);
    }
}
