package com.microsoft.appcenter.http;

import org.json.JSONException;

import java.io.Closeable;
import java.net.URL;
import java.util.Map;

public interface HttpClient extends Closeable {

    ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback);

    interface CallTemplate {
        String buildRequestBody() throws JSONException;

        void onBeforeCalling(URL url, Map<String, String> headers);
    }
}
