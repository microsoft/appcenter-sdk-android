package com.microsoft.appcenter.storage.client;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.storage.Constants.LOG_TAG;

public class TokenExchange {
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
}
