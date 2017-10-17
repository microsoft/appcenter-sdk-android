package com.microsoft.azure.mobile.push;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PushIntentUtils {
    //TODO double check these
    static final String EXTRA_TITLE = "gcm.notification.title";
    static final String EXTRA_MESSAGE = "gcm.notification.message";
    static final String EXTRA_COLOR = "gcm.notification.color";
    static final String EXTRA_SOUND = "gcm.notification.sound";
    static final String EXTRA_ICON = "gcm.notification.icon";

    static final String EXTRA_IGNORE_PREFIX = "gcm.notification";

    /**
     * Google message identifier extra intent key.
     */
    @VisibleForTesting
    static final String EXTRA_GOOGLE_MESSAGE_ID = "google.message_id";

    // TODO also need to filter out keys that start with gcm
    /**
     * Intent extras not part of custom data.
     */
    @VisibleForTesting
    static final Set<String> EXTRA_STANDARD_KEYS = new HashSet<String>() {
        {
            add(EXTRA_GOOGLE_MESSAGE_ID);
            add("google.sent_time");
            add("collapse_key");
            add("from");
        }
    };

    public static Map<String, String> getCustomData(Intent pushIntent) {
        Bundle intentExtras = pushIntent.getExtras();
        Set<String> intentKeys = intentExtras.keySet();
        intentKeys.removeAll(EXTRA_STANDARD_KEYS);
        Map<String, String> customData = new HashMap<>();
        for (String key : intentKeys) {
            if (key.startsWith(EXTRA_IGNORE_PREFIX)) {
                continue;
            }
            customData.put(key, intentExtras.getString(key));
        }
        return customData;
    }

    public static String getTitle(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_TITLE);
    }

    public static String getMessage(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_MESSAGE);
    }

    public static String getGoogleMessageId(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_GOOGLE_MESSAGE_ID);
    }

    public static String getSound(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_SOUND);
    }

    public static String getColor(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_COLOR);
    }

    public static String getIcon(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_ICON);
    }
}
