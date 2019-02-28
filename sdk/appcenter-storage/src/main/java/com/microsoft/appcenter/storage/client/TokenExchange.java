package com.microsoft.appcenter.storage.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.TokenManager;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Utils.handleApiCallFailure;

public final class TokenExchange {

    /**
     * Check latest public release API URL path. Contains the app secret variable to replace.
     */
    public static final String GET_TOKEN_PATH_FORMAT = "/data/tokens";

    /**
     * App Secret Header
     */
    public static final String APP_SECRET_HEADER = "App-Secret";


    /**
     * Build the request body to get the token through http client.
     * @param partition The partition names.
     * @return request Body to get the token.
     */
    public static String buildAppCenterGetDbTokenBodyPayload(final String partition) {
        JsonArray partitionsArray = new JsonArray();
        partitionsArray.add(partition);

        JsonObject partitionsObject = new JsonObject();
        partitionsObject.add("partitions", partitionsArray);

        return partitionsObject.toString();
    }

    /**
     * Get the token access to specific partition.
     * @param partition  The partition names.
     * @param httpClient Httpclient used to make the call.
     * @param apiUrl Api endpoint.
     * @param appSecret App secret.
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
        TokenResult cachedToken = TokenManager.getInstance().getCachedToken(partition);
        if (cachedToken != null) {
            serviceCallback.callCosmosDb(cachedToken);

            /* Get the token from local cache directly, not need to make http call, build the object and return. */
            return new ServiceCall() {

                @Override
                public void cancel() {
                }
            };
        } else {
            return
                    httpClient.callAsync(
                            url,
                            METHOD_POST,
                            new HashMap<String, String>() {
                                {
                                    put(APP_SECRET_HEADER, appSecret);
                                }
                            },
                            new HttpClient.CallTemplate() {

                                @Override
                                public String buildRequestBody() {
                                    return buildAppCenterGetDbTokenBodyPayload(partition);
                                }

                                @Override
                                public void onBeforeCalling(URL url, Map<String, String> headers) {
                                }
                            },
                            serviceCallback);
        }
    }

    /**
     * The service callback for get the token.
     */
    public abstract static class TokenExchangeServiceCallback implements ServiceCallback {
        @Override
        public void onCallSucceeded(String payload, Map<String, String> headers) {
            final TokenResult tokenResult = parseTokenResult(payload);
            callCosmosDb(tokenResult);
        }

        @Override
        public void onCallFailed(Exception e) {
            handleApiCallFailure(e);
            completeFuture(e);
        }

        private TokenResult parseTokenResult(String payload) {
            TokensResponse tokensResponse = Utils.sGson.fromJson(payload, TokensResponse.class);

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

        public abstract void callCosmosDb(final TokenResult tokenResult);
    }
}
