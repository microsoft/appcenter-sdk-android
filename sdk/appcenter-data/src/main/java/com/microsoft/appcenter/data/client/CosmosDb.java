/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.client;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.data.Constants;
import com.microsoft.appcenter.data.models.TokenResult;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;

public class CosmosDb {

    /**
     * Document DB base endpoint
     */
    private static final String DOCUMENT_DB_ENDPOINT = "https://%s.documents.azure.com";

    /**
     * Document DB database URL suffix
     */
    private static final String DOCUMENT_DB_DATABASE_URL_SUFFIX = "dbs/%s";

    /**
     * Document DB collection URL suffix
     */
    private static final String DOCUMENT_DB_COLLECTION_URL_SUFFIX = "colls/%s";

    /**
     * Document DB document URL suffix
     */
    private static final String DOCUMENT_DB_DOCUMENT_URL_PREFIX = "docs";

    /**
     * Cosmos DB upsert header.
     */
    private static final String X_MS_DOCUMENTDB_IS_UPSERT = "x-ms-documentdb-is-upsert";

    /**
     * Returns Current Time in RFC 1123 format, e.g,
     * Fri, 01 Dec 2017 19:22:30 GMT.
     *
     * @return an instance of String
     */
    private static String nowAsRFC1123() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(new Date()).toLowerCase();
    }

    private static String urlEncode(String url) {
        return urlEncode(url, "UTF-8");
    }

    @VisibleForTesting
    public static String urlEncode(String url, String enc) {
        try {
            return URLEncoder.encode(url, enc);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Failed to encode url " + url, e);
        }
    }

    public static Map<String, String> addRequiredHeaders(
            Map<String, String> additionalHeaders,
            final String partition,
            final String dbToken) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ms-documentdb-partitionkey", String.format("[\"%s\"]", partition));
        headers.put("x-ms-version", "2018-06-18");
        headers.put("x-ms-date", nowAsRFC1123());
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", urlEncode(dbToken));
        if (additionalHeaders != null) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }

    private static String getDocumentDbEndpoint(String dbAccount, String documentResourceId) {
        return String.format(DOCUMENT_DB_ENDPOINT, dbAccount) + "/" +
                documentResourceId;
    }


    public static String getDocumentBaseUrl(String databaseName, String collectionName, String documentId) {
        return String.format(DOCUMENT_DB_DATABASE_URL_SUFFIX, urlEncode(databaseName)) + "/" +
                String.format(DOCUMENT_DB_COLLECTION_URL_SUFFIX, urlEncode(collectionName)) + "/" +
                DOCUMENT_DB_DOCUMENT_URL_PREFIX + (documentId == null ? "" : '/' + urlEncode(documentId));
    }

    private static String getDocumentUrl(TokenResult tokenResult, String documentId) {
        String documentResourceIdPrefix = getDocumentBaseUrl(tokenResult.getDbName(), tokenResult.getDbCollectionName(), documentId);
        return getDocumentDbEndpoint(tokenResult.getDbAccount(), documentResourceIdPrefix);
    }

    public static synchronized ServiceCall callCosmosDbListApi(
            TokenResult tokenResult,
            String continuationToken,
            HttpClient httpClient,
            ServiceCallback serviceCallback) {
        Map<String, String> headers = addRequiredHeaders(null, tokenResult.getPartition(), tokenResult.getToken());
        if (continuationToken != null) {
            headers.put(Constants.CONTINUATION_TOKEN_HEADER, continuationToken);
        }
        return callApi(
                METHOD_GET,
                getDocumentUrl(tokenResult, null),
                headers,
                null,
                httpClient,
                serviceCallback
        );
    }

    public static synchronized ServiceCall callCosmosDbApi(
            TokenResult tokenResult,
            String documentId,
            HttpClient httpClient,
            String httpVerb,
            String body,
            ServiceCallback serviceCallback) {
        return callCosmosDbApi(tokenResult, documentId, httpClient, httpVerb, body, new HashMap<String, String>(), serviceCallback);
    }

    public static ServiceCall callCosmosDbApi(
            TokenResult tokenResult,
            String documentId,
            HttpClient httpClient,
            String httpVerb,
            String body,
            Map<String, String> additionalHeaders,
            ServiceCallback serviceCallback) {
        Map<String, String> headers = addRequiredHeaders(additionalHeaders, tokenResult.getPartition(), tokenResult.getToken());
        return callApi(
                httpVerb,
                getDocumentUrl(tokenResult, documentId),
                headers,
                body,
                httpClient,
                serviceCallback);
    }

    public static Map<String, String> getUpsertAdditionalHeader() {
        return new HashMap<String, String>() {{
            put(X_MS_DOCUMENTDB_IS_UPSERT, "true");
        }};
    }

    private static ServiceCall callApi(
            String httpVerb,
            String url,
            Map<String, String> headers,
            final String body,
            HttpClient httpClient,
            ServiceCallback serviceCallback) {
        return httpClient.callAsync(
                url,
                httpVerb,
                headers,
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        return body;
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                    }
                },
                serviceCallback);
    }
}
