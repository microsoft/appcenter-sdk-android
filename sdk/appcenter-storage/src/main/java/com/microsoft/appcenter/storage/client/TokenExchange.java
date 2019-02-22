package com.microsoft.appcenter.storage.client;

import android.content.Context;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONStringer;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;

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

    public static synchronized <T> void getDbToken(final String partition, Context context, String apiUrl, final String appSecret, ServiceCallback serviceCallback) {
        AppCenterLog.debug(LOG_TAG, "Get token from the appcenter service...");
        HttpClient httpClient = createHttpClient(context);
        String url = apiUrl;
        url += String.format(GET_TOKEN_PATH_FORMAT, appSecret);

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
