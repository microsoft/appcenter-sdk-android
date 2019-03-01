package com.microsoft.appcenter.storage.client;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.models.TokenResult;

import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;

public final class CosmosDb {
    /**
     * Document DB base endpoint
     */
    static final String DOCUMENT_DB_ENDPOINT = "https://%s.documents.azure.com";

    /**
     * Document DB database URL suffix
     */
    static final String DOCUMENT_DB_DATABASE_URL_SUFFIX = "dbs/%s";

    /**
     * Document DB collection URL suffix
     */
    static final String DOCUMENT_DB_COLLECTION_URL_SUFFIX = "colls/%s";

    /**
     * Document DB document URL suffix
     */
    static final String DOCUMENT_DB_DOCUMENT_URL_PREFIX = "docs";

    /**
     * Document DB document URL suffix
     */
    static final String DOCUMENT_DB_DOCUMENT_URL_SUFFIX = "docs/%s";

    /**
     * Document DB authorization header format
     * TODO : Change the "type" to be "resource" instead of "master"
     */
    static final String DOCUMENT_DB_AUTHORIZATION_HEADER_FORMAT = "type=master&ver=1.0&sig=%s";

    /**
     * Returns Current Time in RFC 1123 format, e.g,
     * Fri, 01 Dec 2017 19:22:30 GMT.
     *
     * @return an instance of String
     */
    public static String nowAsRFC1123() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(new Date()).toLowerCase();
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to encode url " + url, e);
        }
    }

    public static Map<String, String> generateDefaultHeaders(final String partition, final String dbToken) {
        Map<String, String> headers = new HashMap<String, String>() {{
            put("x-ms-documentdb-partitionkey", String.format("[\"%s\"]", partition));
            put("x-ms-version", "2018-06-18");
            put("x-ms-date", nowAsRFC1123());
            put("Content-Type", "application/json");
            put("Authorization", urlEncode(dbToken));
        }};

        return headers;
    }

    public static String getDocumentDbEndpoint(String dbAccount, String documentResourceId) {
        return String.format(DOCUMENT_DB_ENDPOINT, dbAccount) + "/" +
                documentResourceId;
    }


    public static String getDocumentBaseUrl(String databaseName, String collectionName, String documentId) {
        return String.format(DOCUMENT_DB_DATABASE_URL_SUFFIX, databaseName) + "/" +
                String.format(DOCUMENT_DB_COLLECTION_URL_SUFFIX, collectionName) + "/" +
                DOCUMENT_DB_DOCUMENT_URL_PREFIX + (documentId == null ? "" : '/' + documentId);
    }

    public static String getDocumentUrl(TokenResult tokenResult, String documentId) {
        final String documentResourceIdPrefix = getDocumentBaseUrl(tokenResult.dbName(), tokenResult.dbCollectionName(), documentId);
        return getDocumentDbEndpoint(tokenResult.dbAccount(), documentResourceIdPrefix);
    }

    public static synchronized <T> ServiceCall callCosmosDbListApi(
            TokenResult tokenResult,
            String continuationToken,
            HttpClient httpClient,
            ServiceCallback serviceCallback) {

        Map<String, String> headers = generateDefaultHeaders(tokenResult.partition(), tokenResult.token());
        if (continuationToken != null){
            headers.put("x-ms-continuation", continuationToken);
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

    public static synchronized <T> ServiceCall callCosmosDbApi(
            TokenResult tokenResult,
            String documentId,
            HttpClient httpClient,
            String httpVerb,
            final String body,
            ServiceCallback serviceCallback) {
        return callApi(
                httpVerb,
                getDocumentUrl(tokenResult, documentId),
                generateDefaultHeaders(tokenResult.partition(), tokenResult.token()),
                body,
                httpClient,
                serviceCallback
        );
    }

    private static synchronized <T> ServiceCall callApi(
            String httpVerb,
            String url,
            Map<String, String> headers,
            final String body,
            HttpClient httpClient,
            ServiceCallback serviceCallback) {
        return
                httpClient.callAsync(
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
                        serviceCallback
                );
    }
}
