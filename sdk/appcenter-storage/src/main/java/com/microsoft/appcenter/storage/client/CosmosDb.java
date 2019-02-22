package com.microsoft.appcenter.storage.client;

import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    public static Map<String, String> generateHeaders(String documentResourceId, final String partition, final String dbToken) {
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

    public static String getDocumentUrl(String dbAccount, String documentResourseId) {
        return String.format(DOCUMENT_DB_ENDPOINT, dbAccount) + "/" +
                documentResourseId;
    }

    public static String getDocumentResourceId(String databaseName, String collectionName, String documentId) {
        return String.format(DOCUMENT_DB_DATABASE_URL_SUFFIX, databaseName) + "/" +
        String.format(DOCUMENT_DB_COLLECTION_URL_SUFFIX, collectionName) + "/" +
        String.format(DOCUMENT_DB_DOCUMENT_URL_SUFFIX, documentId);
    }
}
