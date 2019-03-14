/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.appcenter.push.Push.LOG_TAG;

class PushIntentUtils {

    private static final String EXTRA_GCM_PREFIX = "gcm.notification.";

    @VisibleForTesting
    static final String EXTRA_TITLE = EXTRA_GCM_PREFIX + "title";

    @VisibleForTesting
    static final String EXTRA_MESSAGE = EXTRA_GCM_PREFIX + "body";

    @VisibleForTesting
    static final String EXTRA_COLOR = EXTRA_GCM_PREFIX + "color";

    @VisibleForTesting
    static final String EXTRA_SOUND = EXTRA_GCM_PREFIX + "sound";

    @VisibleForTesting
    static final String EXTRA_SOUND_ALT = EXTRA_GCM_PREFIX + "sound2";

    @VisibleForTesting
    static final String EXTRA_ICON = EXTRA_GCM_PREFIX + "icon";

    @VisibleForTesting
    static final String EXTRA_GOOGLE_PREFIX = "google.";

    @VisibleForTesting
    static final String EXTRA_GOOGLE_MESSAGE_ID = EXTRA_GOOGLE_PREFIX + "message_id";


    /**
     * Intent extras not part of custom data.
     */
    @VisibleForTesting
    static final Set<String> EXTRA_STANDARD_KEYS = new HashSet<String>() {
        {
            add("collapse_key");
            add("from");
        }
    };

    /**
     * Gets the custom data from the push intent.
     *
     * @param pushIntent The push intent.
     * @return A map of the custom data entries. Returns an empty map if none exist.
     */
    static Map<String, String> getCustomData(Intent pushIntent) {
        Map<String, String> customData = new HashMap<>();
        Map<String, String> standardData = new HashMap<>();
        Bundle intentExtras = pushIntent.getExtras();
        if (intentExtras != null) {
            for (String key : intentExtras.keySet()) {
                String value = String.valueOf(intentExtras.get(key));
                if (key.startsWith(EXTRA_GCM_PREFIX) || key.startsWith(EXTRA_GOOGLE_PREFIX) || EXTRA_STANDARD_KEYS.contains(key)) {
                    standardData.put(key, value);
                } else {
                    customData.put(key, value);
                }
            }
        }
        AppCenterLog.debug(LOG_TAG, "Push standard data: " + standardData);
        AppCenterLog.debug(LOG_TAG, "Push custom data: " + customData);
        return customData;
    }

    /**
     * Gets the title of the push from an intent.
     *
     * @param pushIntent The push intent.
     * @return The title.
     */
    static String getTitle(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_TITLE);
    }

    /**
     * Gets the message of the push from an intent.
     *
     * @param pushIntent The push intent.
     * @return The message.
     */
    static String getMessage(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_MESSAGE);
    }

    /**
     * Gets the message ID of the push from an intent.
     *
     * @param pushIntent The push intent.
     * @return The message ID.
     */
    static String getMessageId(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_GOOGLE_MESSAGE_ID);
    }

    /**
     * Sets the message ID for an intent.
     *
     * @param messageId  The message ID to set.
     * @param pushIntent The push intent.
     */
    static void setMessageId(String messageId, Intent pushIntent) {
        pushIntent.putExtra(EXTRA_GOOGLE_MESSAGE_ID, messageId);
    }

    /**
     * Gets the name of the sound specified by the push intent.
     *
     * @param pushIntent The push intent.
     * @return The name of the sound, or null if there is none set.
     */
    static String getSound(Intent pushIntent) {
        String sound = pushIntent.getStringExtra(EXTRA_SOUND_ALT);
        return sound == null ? pushIntent.getStringExtra(EXTRA_SOUND) : sound;
    }

    /**
     * Returns the color string that was set in the intent.
     *
     * @param pushIntent The push intent.
     * @return The color a string, null if none was set.
     */
    static String getColor(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_COLOR);
    }

    /**
     * Returns the icon resource ID that was set in the intent.
     *
     * @param pushIntent The push intent.
     * @return The icon ID as a string, null if none was set.
     */
    static String getIcon(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_ICON);
    }
}
