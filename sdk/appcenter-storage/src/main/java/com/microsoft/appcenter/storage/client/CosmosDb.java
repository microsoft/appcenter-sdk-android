package com.microsoft.appcenter.storage.client;

import android.content.Context;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;

public class CosmosDb {
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

    private static final ZoneId GMT_ZONE_ID = ZoneId.of("GMT");
    // NOTE DateTimeFormatter.RFC_1123_DATE_TIME cannot be used.
    // because cosmos db rfc1123 validation requires two digits for day.
    // so Thu, 04 Jan 2018 00:30:37 GMT is accepted by the cosmos db service,
    // but Thu, 4 Jan 2018 00:30:37 GMT is not.
    // Therefore, we need a custom date time formatter.
    private static final DateTimeFormatter RFC_1123_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to encode url " + url, e);
        }
    }

    public static Map<String, String> generateHeaders(final String partition, final String dbToken) {
        Map<String, String> headers = new HashMap<String, String>() {{
            put("x-ms-documentdb-partitionkey", String.format("[\"%s\"]", partition));
            put("x-ms-version", "2018-06-18");
            put("x-ms-date", nowAsRFC1123());
            put("Content-Type", "application/json");
            put("Authorization" , urlEncode(dbToken));
        }};

        return headers;
    }

    /**
     * Returns Current Time in RFC 1123 format, e.g,
     * Fri, 01 Dec 2017 19:22:30 GMT.
     *
     * @return an instance of String
     */
    public static String nowAsRFC1123() {
        ZonedDateTime now = ZonedDateTime.now(GMT_ZONE_ID);
        return RFC_1123_DATE_TIME.format(now);
    }


    public static String getDocumentDbEndpoint(String dbAccount, String documentResourseId) {
        return String.format(DOCUMENT_DB_ENDPOINT, dbAccount) + "/" +
                documentResourseId;
    }


    public static String getDocumentBaseUrl(String databaseName, String collectionName) {
        return String.format(DOCUMENT_DB_DATABASE_URL_SUFFIX, databaseName) + "/" +
                String.format(DOCUMENT_DB_COLLECTION_URL_SUFFIX, collectionName) + "/" +
                DOCUMENT_DB_DOCUMENT_URL_PREFIX;
    }

    public static synchronized <T> void callCosmosDb(
            String dbAccount, String databaseName, String collectionName, String documentId,
            String partition, String token, Context context, String httpVerb, HttpClient.CallTemplate callTemplate,
            ServiceCallback serviceCallback) {
        final String documentResourceIdPrefix = getDocumentBaseUrl(databaseName, collectionName);
        final String url = getDocumentDbEndpoint(dbAccount, documentResourceIdPrefix) + (documentId == null ? "" : '/' + documentId);

        AppCenterLog.debug(LOG_TAG, "Call Cosmos DB to do a " + httpVerb + " on " + url);

        ServiceCall documentResponse =
            createHttpClient(context).callAsync(
                url,
                    httpVerb,
                    generateHeaders(partition, token),
                    callTemplate,
                    serviceCallback
                );
    }
}
