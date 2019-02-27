package com.microsoft.appcenter.storage.client;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.TokenManager;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONStringer;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.handleApiCallFailure;

public class TokenExchange {

    /**
     * Check latest public release API URL path. Contains the app secret variable to replace.
     */
    static final String GET_TOKEN_PATH_FORMAT = "/data/tokens";

    /**
     * App Secret Header
     */
    static final String APP_SECRET_HEADER = "App-Secret";


    public static String buildAppCenterGetDbTokenBodyPayload(final String partition) {
        String apiBody;
        JSONStringer writer = new JSONStringer();
        try {
            // TODO: use https://github.com/google/gson for serialization
            List<String> partitions = new ArrayList<String>() {{add(partition);}};
            writer.object();
            writer.key("partitions").array();
            for (String p : partitions) {
                writer.value(p);
            }
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            AppCenterLog.error(LOG_TAG, "Failed to build API body", e);
        }

        apiBody = writer.toString();
        return apiBody;
    }

    public static synchronized void getDbToken(
            final String partition,
            HttpClient httpClient,
            String apiUrl,
            final String appSecret,
            TokenExchangeServiceCallback serviceCallback) {
        AppCenterLog.debug(LOG_TAG, "Getting a resource token from App Center...");
        String url = apiUrl + GET_TOKEN_PATH_FORMAT;
        TokenResult tokenResult = TokenManager.getInstance().getCachedToken(partition);
        if (tokenResult != null) {
            serviceCallback.callCosmosDb(tokenResult);
        } else {
            ServiceCall tokenResponse =
                    httpClient.callAsync(
                            url,
                            METHOD_POST,
                            new HashMap<String, String>() { { put(APP_SECRET_HEADER, appSecret); } },
                            new HttpClient.CallTemplate() {

                                @Override
                                public String buildRequestBody() {
                                    return buildAppCenterGetDbTokenBodyPayload(partition);
                                }

                                @Override
                                public void onBeforeCalling(URL url, Map<String, String> headers) { }
                            },
                            serviceCallback);
        }
    }

    public abstract static class TokenExchangeServiceCallback implements ServiceCallback {
        @Override
        public void onCallSucceeded(String payload, Map<String, String> headers) {
            final TokenResult tokenResult = parseTokenResult(payload);
            if (!tokenResult.status().equalsIgnoreCase(Constants.SUCCEED)){

                // TODO throws an exception.
                callCosmosDb(null);
            } else {
                TokenManager.getInstance().setCachedToken(tokenResult);
                callCosmosDb(tokenResult);
            }
        }

        @Override
        public void onCallFailed(Exception e) {
            handleApiCallFailure(e);
            completeFuture(e);
        }

        private TokenResult parseTokenResult(String payload) {
            TokensResponse tokensResponse = (new Gson()).fromJson(payload, TokensResponse.class);
            return tokensResponse.tokens().get(0);
        }

        public abstract void completeFuture(Exception e);

        public abstract void callCosmosDb(final TokenResult tokenResult);
    }
}
