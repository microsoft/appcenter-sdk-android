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
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
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
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;

public class Utils {

    private static final Gson sGson = new GsonBuilder().registerTypeAdapter(Date.class, new DateAdapter()).serializeNulls().create();

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

    private static <T> DocumentWrapper<T> parseLocalDocument(LocalDocument localDocument, Class<T> documentType) {
        DocumentWrapper<T> documentWrapper = parseDocument(
                localDocument.getDocument(),
                localDocument.getPartition(),
                localDocument.getDocumentId(),
                localDocument.getETag(),
                localDocument.getOperationTime() / 1000L,
                documentType);
        documentWrapper.setFromCache(true);
        documentWrapper.setPendingOperation(localDocument.getOperation());
        return documentWrapper;
    }

    static <T> PaginatedDocuments<T> localDocumentsToNonExpiredPaginated(Iterable<LocalDocument> localDocuments, Class<T> documentType) {
        PaginatedDocuments<T> paginatedDocuments = new PaginatedDocuments<>();
        Page<T> page = new Page<>();
        ArrayList<DocumentWrapper<T>> documentWrappers = new ArrayList<>();
        for (LocalDocument localDocument : localDocuments) {
            if (!localDocument.isExpired()) {
                documentWrappers.add(parseLocalDocument(localDocument, documentType));
            }
        }
        page.setItems(documentWrappers);
        paginatedDocuments.setCurrentPage(page);
        return paginatedDocuments;
    }

    static <T> DocumentWrapper<T> parseDocument(String cosmosDbPayload, Class<T> documentType) {
        JsonObject body;
        try {
            body = sParser.parse(cosmosDbPayload).getAsJsonObject();
        } catch (RuntimeException e) {
            return new DocumentWrapper<>(e);
        }
        return parseDocument(body, documentType);
    }

    static <T> DocumentWrapper<T> parseDocument(String documentJson, String partition, String documentId, String eTag, long lastUpdatedTime, Class<T> documentType) {
        JsonElement documentElement = documentJson == null ? null : sParser.parse(documentJson);
        return parseDocument(documentElement, partition, documentId, eTag, lastUpdatedTime, documentType);
    }

    static String getETag(String cosmosDbPayload) {
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

    private static <T> DocumentWrapper<T> parseDocument(JsonObject obj, Class<T> documentType) {
        JsonElement document = obj.get(DOCUMENT_FIELD_NAME);
        JsonElement partition = obj.get(PARTITION_KEY_FIELD_NAME);
        JsonElement documentId = obj.get(ID_FIELD_NAME);
        JsonElement eTag = obj.get(ETAG_FIELD_NAME);
        JsonElement timestamp = obj.get(TIMESTAMP_FIELD_NAME);
        if (isNullOrJsonNull(partition) || isNullOrJsonNull(documentId) || isNullOrJsonNull(timestamp)) {
            return new DocumentWrapper<>(new DataException("Failed to deserialize document."));
        }
        return parseDocument(
                document,
                partition.getAsString(),
                documentId.getAsString(),
                !isNullOrJsonNull(eTag) ? eTag.getAsString() : null,
                timestamp.getAsLong(),
                documentType);
    }

    private static <T> DocumentWrapper<T> parseDocument(JsonElement documentObject, String partition, String documentId, String eTag, long lastUpdatedTime, Class<T> documentType) {
        T document = null;
        DataException error = null;
        if (documentObject != null) {
            try {
                document = sGson.fromJson(documentObject, documentType);
            } catch (JsonParseException exception) {
                error = new DataException("Failed to deserialize document.", exception);
            }
        }
        return new DocumentWrapper<>(
                document,
                partition,
                documentId,
                eTag,
                lastUpdatedTime, error);
    }

    private static boolean isNullOrJsonNull(JsonElement value) {
        return value == null || value.isJsonNull();
    }

    public static <T> Page<T> parseDocuments(String cosmosDbPayload, Class<T> documentType) {
        JsonArray array;
        try {
            JsonObject objects = sParser.parse(cosmosDbPayload).getAsJsonObject();
            array = objects.get(Constants.DOCUMENTS_FIELD_NAME).getAsJsonArray();
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to deserialize Page.", e);
            return new Page<>(new DataException("Failed to deserialize Page.", e));
        }
        List<DocumentWrapper<T>> documents = new ArrayList<>();
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
    static synchronized void logApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
    }

    static String removeAccountIdFromPartitionName(String partition) {
        if (partition.equals(DefaultPartitions.APP_DOCUMENTS)) {
            return partition;
        }
        return partition.substring(0, partition.length() - Constants.PARTITION_KEY_SUFFIX_LENGTH);
    }

    public static Gson getGson() {
        return sGson;
    }

    static String getTableName(String partition) {
        if (USER_DOCUMENTS.equals(partition)) {
            return getUserTableName();
        }
        return READONLY_TABLE;
    }

    @NonNull
    static String getTableName(String partition, String accountId) {
        if (USER_DOCUMENTS.equals(partition)) {
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
        if (tokenResult.getPartition().startsWith(DefaultPartitions.USER_DOCUMENTS)) {
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

    public static boolean isValidTokenResult(TokenResult tokenResult) {
        return tokenResult.getDbAccount() != null &&
                tokenResult.getDbName() != null &&
                tokenResult.getDbCollectionName() != null &&
                tokenResult.getToken() != null;
    }
}
