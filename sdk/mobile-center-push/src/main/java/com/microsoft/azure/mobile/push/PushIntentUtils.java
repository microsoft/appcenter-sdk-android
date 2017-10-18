package com.microsoft.azure.mobile.push;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PushIntentUtils {

    /**
     * Intent keys.
     */
    static final String EXTRA_GCM_PREFIX = "gcm.notification.";
    static final String EXTRA_TITLE = EXTRA_GCM_PREFIX + "title";
    static final String EXTRA_MESSAGE = EXTRA_GCM_PREFIX + "body";
    static final String EXTRA_COLOR =  EXTRA_GCM_PREFIX + "color";
    static final String EXTRA_SOUND = EXTRA_GCM_PREFIX + "sound";
    static final String EXTRA_CUSTOM_SOUND = EXTRA_GCM_PREFIX + "sound2";
    static final String EXTRA_ICON = EXTRA_GCM_PREFIX + "icon";
    static final String EXTRA_GOOGLE_MESSAGE_ID = "google.message_id";

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
            if (key.startsWith(EXTRA_GCM_PREFIX)) {
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

    public static String getCustomSound(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_CUSTOM_SOUND);
    }

    public static boolean useDefaultSound(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_SOUND) != null;
    }

    //Null means no color
    public static String getColor(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_COLOR);
    }

    // Null means no icon
    public static String getIcon(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_ICON);
    }
}
