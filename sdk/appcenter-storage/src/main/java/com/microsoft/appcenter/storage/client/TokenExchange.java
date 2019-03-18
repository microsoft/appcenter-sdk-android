/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.TokenManager;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.Constants.APP_SECRET;
import static com.microsoft.appcenter.Constants.AUTHORIZATION_HEADER;
import static com.microsoft.appcenter.Constants.AUTH_TOKEN_FORMAT;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Utils.handleApiCallFailure;

public class TokenExchange {

    /**
     * Check latest public release API URL path. Contains the app secret variable to replace.
     */
    public static final String GET_TOKEN_PATH_FORMAT = "/data/tokens";

    /**
     * Build the request body to get the token through http client.
     *
     * @param partition The partition names.
     * @return Request body to get the token.
     */
    public static String buildAppCenterGetDbTokenBodyPayload(String partition) {
        JsonArray partitionsArray = new JsonArray();
        partitionsArray.add(partition);
        JsonObject partitionsObject = new JsonObject();
        partitionsObject.add("partitions", partitionsArray);
        return partitionsObject.toString();
    }

    /**
     * Get the token access to specific partition.
     *
     * @param partition       The partition names.
     * @param httpClient      Httpclient used to make the call.
     * @param apiUrl          Api endpoint.
     * @param appSecret       App secret.
     * @param serviceCallback The callback to execute when get the token successfully.
     * @return The service call to get the token.
     */
    public static synchronized ServiceCall getDbToken(
            final String partition,
            HttpClient httpClient,
            String apiUrl,
            final String appSecret,
            TokenExchangeServiceCallback serviceCallback) {
        AppCenterLog.debug(LOG_TAG, "Getting a resource token from App Center...");
        String url = apiUrl + GET_TOKEN_PATH_FORMAT;
        final String userToken = AuthTokenContext.getInstance().getAuthToken();
        return httpClient.callAsync(
                url,
                METHOD_POST,
                new HashMap<String, String>() {
                    {
                        put(APP_SECRET, appSecret);
                        if (userToken != null) {
                            put(AUTHORIZATION_HEADER, String.format(AUTH_TOKEN_FORMAT, userToken));
                        }
                    }
                },
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        return buildAppCenterGetDbTokenBodyPayload(partition);
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                        if (AppCenterLog.getLogLevel() <= VERBOSE) {

                            /* Log url. */
                            AppCenterLog.verbose(AppCenter.LOG_TAG, "Calling " + url + "...");

                            /* Log headers. */
                            Map<String, String> logHeaders = new HashMap<>(headers);
                            String appSecret = logHeaders.get(APP_SECRET);
                            if (appSecret != null) {
                                logHeaders.put(APP_SECRET, HttpUtils.hideSecret(appSecret));
                            }
                            String authToken = logHeaders.get(AUTHORIZATION_HEADER);
                            if (authToken != null) {
                                logHeaders.put(AUTHORIZATION_HEADER, HttpUtils.hideAuthToken(authToken));
                            }
                            AppCenterLog.verbose(AppCenter.LOG_TAG, "Headers: " + logHeaders);
                        }
                    }
                },
                serviceCallback);
    }

    /**
     * The service callback for get the token.
     */
    public abstract static class TokenExchangeServiceCallback implements ServiceCallback {

        @Override
        public void onCallSucceeded(String payload, Map<String, String> headers) {
            TokenResult tokenResult = parseTokenResult(payload);
            if (tokenResult == null) {
                String message = "Call to App Center Token Exchange Service succeeded but the resulting payload indicates a failed state: " + payload;
                onCallFailed(new StorageException(message));
            } else {
                callCosmosDb(tokenResult);
            }
        }

        @Override
        public void onCallFailed(Exception e) {
            handleApiCallFailure(e);
            completeFuture(e);
        }

        private TokenResult parseTokenResult(String payload) {
            TokensResponse tokensResponse = Utils.getGson().fromJson(payload, TokensResponse.class);
            if (tokensResponse != null &&
                    tokensResponse.tokens() != null &&
                    tokensResponse.tokens().size() == 1 &&
                    tokensResponse.tokens().get(0).status().equalsIgnoreCase(Constants.TOKEN_RESULT_SUCCEED)) {
                TokenResult tokenResult = tokensResponse.tokens().get(0);
                TokenManager.getInstance().setCachedToken(tokenResult);
                return tokenResult;
            }
            return null;
        }

        public abstract void completeFuture(Exception e);

        public abstract void callCosmosDb(TokenResult tokenResult);
    }
}
