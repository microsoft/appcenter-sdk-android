/*
 * The MIT License (MIT)
 * Copyright (c) 2018 Microsoft Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.appcenter.storage.cosmosdb;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.security.SignatureException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import android.text.TextUtils;
import android.util.Pair;


/**
 * This class is used internally by both client (for generating the auth header with master/system key) and by the Gateway when
 * verifying the auth header in the Azure Cosmos DB database service.
 */
public class BaseAuthorizationTokenProvider implements AuthorizationTokenProvider {
    public static final String StringArgumentNullOrEmpty = "String agument %s is null or empty";

    private static final String AUTH_PREFIX = "type=master&ver=1.0&sig=";
    private final String masterKey;
    private final byte[] masterKeyDecodedBytes;

    public BaseAuthorizationTokenProvider(String masterKey) {
        this.masterKey = masterKey;
        masterKeyDecodedBytes = Utils.Base64Decoder.decode(this.masterKey.getBytes());
    }

    private static String getResourceSegment(ResourceType resourceType) {
        switch (resourceType) {
        case Attachment:
            return Paths.ATTACHMENTS_PATH_SEGMENT;
        case Database:
            return Paths.DATABASES_PATH_SEGMENT;
        case Conflict:
            return Paths.CONFLICTS_PATH_SEGMENT;
        case Document:
            return Paths.DOCUMENTS_PATH_SEGMENT;
        case DocumentCollection:
            return Paths.COLLECTIONS_PATH_SEGMENT;
        case Offer:
            return Paths.OFFERS_PATH_SEGMENT;
        case Permission:
            return Paths.PERMISSIONS_PATH_SEGMENT;
        case StoredProcedure:
            return Paths.STORED_PROCEDURES_PATH_SEGMENT;
        case Trigger:
            return Paths.TRIGGERS_PATH_SEGMENT;
        case UserDefinedFunction:
            return Paths.USER_DEFINED_FUNCTIONS_PATH_SEGMENT;
        case User:
            return Paths.USERS_PATH_SEGMENT;
        case PartitionKeyRange:
            return Paths.PARTITION_KEY_RANGES_PATH_SEGMENT;
        case Media:
            return Paths.MEDIA_PATH_SEGMENT;
        case DatabaseAccount:
            return "";
        default:
            return null;
        }
    }

    /**
     * This API is a helper method to create auth header based on client request using masterkey.
     *
     * @param verb                 the verb.
     * @param resourceIdOrFullName the resource id or full name
     * @param resourceType         the resource type.
     * @param headers              the request headers.
     * @return the key authorization signature.
     */
    public String generateKeyAuthorizationSignature(String verb,
            String resourceIdOrFullName,
            ResourceType resourceType,
            Map<String, String> headers) throws SignatureException {
        return this.generateKeyAuthorizationSignature(
                verb,
                resourceIdOrFullName,
                BaseAuthorizationTokenProvider.getResourceSegment(resourceType).toLowerCase(),
                headers);
    }

    public static boolean isNameBased(String resourceIdOrFullName) {
        // quick way to tell whether it is resourceId nor not, non conclusively.
        if (resourceIdOrFullName != null && !resourceIdOrFullName.isEmpty()
                && resourceIdOrFullName.length() > 4 && resourceIdOrFullName.charAt(3) == '/') {
            return true;
        }
        return false;
    }

    /**
     * This API is a helper method to create auth header based on client request using masterkey.
     *
     * @param verb                 the verb
     * @param resourceIdOrFullName the resource id or full name
     * @param  resourceSegment     the resource segment
     * @param headers              the request headers
     * @return the key authorization signature
     */
    public String generateKeyAuthorizationSignature(String verb,
            String resourceIdOrFullName,
            String resourceSegment,
            Map<String, String> headers) throws SignatureException {
        if (verb == null || verb.isEmpty()) {
            throw new IllegalArgumentException("verb");
        }

        if (resourceIdOrFullName == null) {
            resourceIdOrFullName = "";
        }

        if (resourceSegment == null) {
            throw new IllegalArgumentException("resourceSegment");
        }

        if (headers == null) {
            throw new IllegalArgumentException("headers");
        }

        if (this.masterKey == null || this.masterKey.isEmpty()) {
            throw new IllegalArgumentException("masterKey");
        }

        if(!isNameBased(resourceIdOrFullName)) {
            resourceIdOrFullName = resourceIdOrFullName.toLowerCase(Locale.ROOT);
        }

        // Skipping lower casing of resourceId since it may now contain "ID" of the resource as part of the FullName
        StringBuilder body = new StringBuilder();
        body.append(verb.toLowerCase())
                .append('\n')
                .append(resourceSegment)
                .append('\n')
                .append(resourceIdOrFullName)
                .append('\n');

        if (headers.containsKey(HttpConstants.HttpHeaders.X_DATE)) {
            body.append(headers.get(HttpConstants.HttpHeaders.X_DATE).toLowerCase());
        }

        body.append('\n');

        if (headers.containsKey(HttpConstants.HttpHeaders.HTTP_DATE)) {
            body.append(headers.get(HttpConstants.HttpHeaders.HTTP_DATE).toLowerCase());
        }

        body.append('\n');

        String auth = Utils.encodeBase64String(Utils.hashMacToByte(body.toString(), masterKeyDecodedBytes));

        return AUTH_PREFIX + auth;
    }

    /**
     * This API is a helper method to create auth header based on client request using resourceTokens.
     *
     * @param resourceTokens the resource tokens.
     * @param path           the path.
     * @param resourceId     the resource id.
     * @return the authorization token.
     */
    public String getAuthorizationTokenUsingResourceTokens(Map<String, String> resourceTokens,
            String path,
            String resourceId) {
        if (resourceTokens == null) {
            throw new IllegalArgumentException("resourceTokens");
        }

        String resourceToken = null;
        if (resourceTokens.containsKey(resourceId) && resourceTokens.get(resourceId) != null) {
            resourceToken = resourceTokens.get(resourceId);
        } else if (TextUtils.isEmpty(path) || TextUtils.isEmpty(resourceId)) {
            if (resourceTokens.size() > 0) {
                resourceToken = resourceTokens.values().iterator().next();
            }
        } else {
            // Get the last resource id from the path and use that to find the corresponding token.
            String[] pathParts = TextUtils.split(path, "/");
            String[] resourceTypes = {"dbs", "colls", "docs", "sprocs", "udfs", "triggers", "users", "permissions",
                    "attachments", "media", "conflicts"};
            HashSet<String> resourceTypesSet = new HashSet<String>();
            Collections.addAll(resourceTypesSet, resourceTypes);

            for (int i = pathParts.length - 1; i >= 0; --i) {

                if (!resourceTypesSet.contains(pathParts[i]) && resourceTokens.containsKey(pathParts[i])) {
                    resourceToken = resourceTokens.get(pathParts[i]);
                }
            }
        }

        return resourceToken;
    }
    public String generateKeyAuthorizationSignature(String verb, URI uri, Map<String, String> headers) throws SignatureException {
        if (TextUtils.isEmpty(verb)) {
            throw new IllegalArgumentException(String.format(StringArgumentNullOrEmpty, "verb"));
        }

        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }

        if (headers == null) {
            throw new IllegalArgumentException("headers");
        }
        PathInfo pathInfo = new PathInfo(false, "", "", false);
        getResourceTypeAndIdOrFullName(uri, pathInfo);
        return generateKeyAuthorizationSignatureNew(verb, pathInfo.resourceIdOrFullName, pathInfo.resourcePath,
                headers);
    }

    public String generateKeyAuthorizationSignatureNew(String verb, String resourceIdValue, String resourceType,
            Map<String, String> headers) throws SignatureException {
        if (TextUtils.isEmpty(verb)) {
            throw new IllegalArgumentException(String.format(StringArgumentNullOrEmpty, "verb"));
        }

        if (resourceType == null) {
            throw new IllegalArgumentException(String.format(StringArgumentNullOrEmpty, "resourceType")); // can be empty
        }

        if (headers == null) {
            throw new IllegalArgumentException("headers");
        }
        // Order of the values included in the message payload is a protocol that
        // clients/BE need to follow exactly.
        // More headers can be added in the future.
        // If any of the value is optional, it should still have the placeholder value
        // of ""
        // OperationType -> ResourceType -> ResourceId/OwnerId -> XDate -> Date
        String verbInput = verb;
        String resourceIdInput = resourceIdValue;
        String resourceTypeInput = resourceType;

        String authResourceId = getAuthorizationResourceIdOrFullName(resourceTypeInput, resourceIdInput);
        String payLoad = generateMessagePayload(verbInput, authResourceId, resourceTypeInput, headers);


        String authorizationToken = Utils.encodeBase64String(Utils.hashMacToByte(payLoad, masterKeyDecodedBytes));
        String authtoken = AUTH_PREFIX + authorizationToken;
        return Utils.urlEncode(authtoken);
    }

    private String generateMessagePayload(String verb, String resourceId, String resourceType,
            Map<String, String> headers) {
        String xDate = headers.get(HttpConstants.HttpHeaders.X_DATE);
        String date = headers.get(HttpConstants.HttpHeaders.HTTP_DATE);
        // At-least one of date header should present
        // https://docs.microsoft.com/en-us/rest/api/documentdb/access-control-on-documentdb-resources
        if (TextUtils.isEmpty(xDate) && (TextUtils.isEmpty(date) || date == " ")) {
            headers.put(HttpConstants.HttpHeaders.X_DATE, Utils.nowAsRFC1123());
            xDate = Utils.nowAsRFC1123();
        }

        // for name based, it is case sensitive, we won't use the lower case
        if (!isNameBased(resourceId)) {
            resourceId = resourceId.toLowerCase();
        }

        StringBuilder payload = new StringBuilder();
        payload.append(verb.toLowerCase())
                .append('\n')
                .append(resourceType.toLowerCase())
                .append('\n')
                .append(resourceId)
                .append('\n')
                .append(xDate.toLowerCase())
                .append('\n')
                .append(TextUtils.isEmpty(xDate) ? date.toLowerCase() : "")
                .append('\n');

        return payload.toString();
    }

    private String getAuthorizationResourceIdOrFullName(String resourceType, String resourceIdOrFullName) {
        if (TextUtils.isEmpty(resourceType) || TextUtils.isEmpty(resourceIdOrFullName)) {
            return resourceIdOrFullName;
        }
        if (isNameBased(resourceIdOrFullName)) {
            // resource fullname is always end with name (not type segment like docs/colls).
            return resourceIdOrFullName;
        }

        if (resourceType.equalsIgnoreCase(Paths.OFFERS_PATH_SEGMENT)
                || resourceType.equalsIgnoreCase(Paths.PARTITIONS_PATH_SEGMENT)
                || resourceType.equalsIgnoreCase(Paths.TOPOLOGY_PATH_SEGMENT)
                || resourceType.equalsIgnoreCase(Paths.RID_RANGE_PATH_SEGMENT)) {
            return resourceIdOrFullName;
        }

        ResourceId parsedRId = ResourceId.parse(resourceIdOrFullName);
        if (resourceType.equalsIgnoreCase(Paths.DATABASES_PATH_SEGMENT)) {
            return parsedRId.getDatabaseId().toString();
        } else if (resourceType.equalsIgnoreCase(Paths.USERS_PATH_SEGMENT)) {
            return parsedRId.getUserId().toString();
        } else if (resourceType.equalsIgnoreCase(Paths.COLLECTIONS_PATH_SEGMENT)) {
            return parsedRId.getDocumentCollectionId().toString();
        } else if (resourceType.equalsIgnoreCase(Paths.DOCUMENTS_PATH_SEGMENT)) {
            return parsedRId.getDocumentId().toString();
        } else {
            // leaf node
            return resourceIdOrFullName;
        }
    }

    public static final String PATH_SEPARATOR = "/";

    private void getResourceTypeAndIdOrFullName(URI uri, PathInfo pathInfo) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }

        pathInfo.resourcePath = "";
        pathInfo.resourceIdOrFullName = "";

        String[] segments = TextUtils.split(uri.toString(), PATH_SEPARATOR);
        if (segments == null || segments.length < 1) {
            throw new IllegalArgumentException("InvalidUrl");
        }
        // Authorization code is fine with Uri not having resource id and path.
        // We will just return empty in that case
        String pathAndQuery = "" ;
        if(!TextUtils.isEmpty(uri.getPath())) {
            pathAndQuery+= uri.getPath();
        }
        if(!TextUtils.isEmpty(uri.getQuery())) {
            pathAndQuery+="?";
            pathAndQuery+= uri.getQuery();
        }
        if (!tryParsePathSegments(pathAndQuery, pathInfo, null)) {
            pathInfo.resourcePath = "";
            pathInfo.resourceIdOrFullName = "";
        }
    }

    /**
     * Method which will return boolean based on whether it is able to parse the
     * path and name segment from resource url , and fill info in PathInfo object
     * @param resourceUrl  Complete ResourceLink 
     * @param pathInfo Path info object which will hold information
     * @param clientVersion The Client version
     * @return
     */
    public static boolean tryParsePathSegments(String resourceUrl, PathInfo pathInfo, String clientVersion) {
        pathInfo.resourcePath = "";
        pathInfo.resourceIdOrFullName = "";
        pathInfo.isFeed = false;
        pathInfo.isNameBased = false;
        if (TextUtils.isEmpty(resourceUrl)) {
            return false;
        }
        String trimmedStr = resourceUrl.replaceAll("/$", "");
        String[] segments = TextUtils.split(trimmedStr, PATH_SEPARATOR);
        if (segments == null || segments.length < 1) {
            return false;
        }
        int uriSegmentsCount = segments.length;
        String segmentOne = segments[uriSegmentsCount - 1];
        String segmentTwo = (uriSegmentsCount >= 2) ? segments[uriSegmentsCount - 2] : "";

        // handle name based operation
        if (uriSegmentsCount >= 2) {
            // parse the databaseId, if failed, it is name based routing
            // mediaId is special, we will treat it always as id based.
            if (Paths.MEDIA_PATH_SEGMENT.compareTo(segments[0]) != 0
                    && Paths.OFFERS_PATH_SEGMENT.compareTo(segments[0]) != 0
                    && Paths.PARTITIONS_PATH_SEGMENT.compareTo(segments[0]) != 0
                    && Paths.DATABASE_ACCOUNT_PATH_SEGMENT.compareTo(segments[0]) != 0
                    && Paths.TOPOLOGY_PATH_SEGMENT.compareTo(segments[0]) != 0
                    && Paths.RID_RANGE_PATH_SEGMENT.compareTo(segments[0]) != 0) {
                Pair<Boolean, ResourceId> result = ResourceId.tryParse(segments[1]);
                if (!result.first || !result.second.isDatabaseId()) {
                    pathInfo.isNameBased = true;
                    return tryParseNameSegments(resourceUrl, segments, pathInfo);
                }
            }
        }
        // Feed paths have odd number of segments
        if ((uriSegmentsCount % 2 != 0) && isResourceType(segmentOne)) {
            pathInfo.isFeed = true;
            pathInfo.resourcePath = segmentOne;
            // The URL for dbs may contain the management endpoint as the segmentTwo which
            // should not be used as resourceId
            if (!segmentOne.equalsIgnoreCase(Paths.DATABASES_PATH_SEGMENT)) {
                pathInfo.resourceIdOrFullName = segmentTwo;
            }
        } else if (isResourceType(segmentTwo)) {
            pathInfo.isFeed = false;
            pathInfo.resourcePath = segmentTwo;
            pathInfo.resourceIdOrFullName = segmentOne;
            // Media ID is not supposed to be used for any ID verification. However, if the
            // old client makes a call for media ID
            // we still need to support it.
            // For new clients, parse to return the attachment id. For old clients do not
            // modify.
            if (!TextUtils.isEmpty(clientVersion)
                    && pathInfo.resourcePath.equalsIgnoreCase(Paths.MEDIA_PATH_SEGMENT)) {
                String attachmentId = null;
                byte storeIndex = 0;
                // MEDIA Id parsing code  will come here , supported MediaIdHelper file missing in java sdk(Sync and Async both)
                //Below code from .net 
                // if (!MediaIdHelper.TryParseMediaId(resourceIdOrFullName, out attachmentId, out storeIndex))
                //  {
                //    return false;
                //}
                //resourceIdOrFullName = attachmentId;
            }
        } else {
            return false;
        }

        return true;

    }

    /**
     * Method which will return boolean based on whether it is able to parse the
     * name segment from resource url , and fill info in PathInfo object
     * @param resourceUrl  Complete ResourceLink
     * @param segments
     * @param pathInfo Path info object which will hold information
     * @return
     */
    private static boolean tryParseNameSegments(String resourceUrl, String[] segments, PathInfo pathInfo) {
        pathInfo.isFeed = false;
        pathInfo.resourceIdOrFullName = "";
        pathInfo.resourcePath = "";
        if (segments == null || segments.length < 1) {
            return false;
        }
        if (segments.length % 2 == 0) {
            // even number, assume it is individual resource
            if (isResourceType(segments[segments.length - 2])) {
                pathInfo.resourcePath = segments[segments.length - 2];
                pathInfo.resourceIdOrFullName =
                        //String.unescapeJava(
                                resourceUrl.replaceAll("/$", "").replaceAll("^/", "");
                return true;
            }
        } else {
            // odd number, assume it is feed request
            if (isResourceType(segments[segments.length - 1])) {
                pathInfo.isFeed = true;
                pathInfo.resourcePath = segments[segments.length - 1];
                String resourceIdOrFullName = resourceUrl.replaceAll("/$", "").replaceAll("^/", "");
                pathInfo.resourceIdOrFullName = //StringEscapeUtils.unescapeJava(TextUtils.removeEnd(
                        resourceUrl.replaceAll("/$", "").replaceAll("^/", "");
                return true;
            }
        }
        return false;
    }

    private static boolean isResourceType(String resourcePathSegment) {
        if (TextUtils.isEmpty(resourcePathSegment)) {
            return false;
        }

        switch (resourcePathSegment.toLowerCase()) {
            case Paths.ATTACHMENTS_PATH_SEGMENT:
            case Paths.COLLECTIONS_PATH_SEGMENT:
            case Paths.DATABASES_PATH_SEGMENT:
            case Paths.PERMISSIONS_PATH_SEGMENT:
            case Paths.USERS_PATH_SEGMENT:
            case Paths.DOCUMENTS_PATH_SEGMENT:
            case Paths.STORED_PROCEDURES_PATH_SEGMENT:
            case Paths.TRIGGERS_PATH_SEGMENT:
            case Paths.USER_DEFINED_FUNCTIONS_PATH_SEGMENT:
            case Paths.CONFLICTS_PATH_SEGMENT:
            case Paths.MEDIA_PATH_SEGMENT:
            case Paths.OFFERS_PATH_SEGMENT:
            case Paths.PARTITIONS_PATH_SEGMENT:
            case Paths.DATABASE_ACCOUNT_PATH_SEGMENT:
            case Paths.TOPOLOGY_PATH_SEGMENT:
            case Paths.PARTITION_KEY_RANGES_PATH_SEGMENT:
            case Paths.SCHEMAS_PATH_SEGMENT:
                return true;
            default:
                return false;
        }
    }
}
