package com.microsoft.azure.mobile.push;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PushIntentUtils {

    /**
     * Intent keys.
     */
    @VisibleForTesting
    static final String EXTRA_GCM_PREFIX = "gcm.notification.";

    @VisibleForTesting
    static final String EXTRA_TITLE = EXTRA_GCM_PREFIX + "title";

    @VisibleForTesting
    static final String EXTRA_MESSAGE = EXTRA_GCM_PREFIX + "body";

    @VisibleForTesting
    static final String EXTRA_COLOR =  EXTRA_GCM_PREFIX + "color";

    @VisibleForTesting
    static final String EXTRA_SOUND = EXTRA_GCM_PREFIX + "sound";

    @VisibleForTesting
    static final String EXTRA_CUSTOM_SOUND = EXTRA_GCM_PREFIX + "sound2";

    @VisibleForTesting
    static final String EXTRA_ICON = EXTRA_GCM_PREFIX + "icon";

    @VisibleForTesting
    static final String EXTRA_GOOGLE_MESSAGE_ID = "google.message_id";

    /**
     * Intent extras not part of custom data.
     */
    @VisibleForTesting
    private static final Set<String> EXTRA_STANDARD_KEYS = new HashSet<String>() {
        {
            add(EXTRA_GOOGLE_MESSAGE_ID);
            add("google.sent_time");
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
     * Gets the Google Message ID of the push from an intent.
     *
     * @param pushIntent The push intent.
     * @return The message ID.
     */
    static String getGoogleMessageId(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_GOOGLE_MESSAGE_ID);
    }

    /**
     * Sets the Google Message ID for an intent.
     *
     * @param messageId The message ID to set.
     * @param pushIntent The push intent.
     */
    static void setGoogleMessageId(String messageId, Intent pushIntent) {
        pushIntent.putExtra(EXTRA_GOOGLE_MESSAGE_ID, messageId);
    }

    /**
     * Gets the name of the custom sound specified by the push intent.
     *
     * @param pushIntent The push intent.
     * @return The name of the custom sound, or null if there is none set.
     */
     static String getCustomSound(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_CUSTOM_SOUND);
    }

    /**
     * Returns whether a sound should be played by the notification.
     *
     * @param pushIntent The push intent.
     * @return 'true' if a sound should be played, 'false' otherwise.
     */
     static boolean useAnySound(Intent pushIntent) {
        return pushIntent.getStringExtra(EXTRA_SOUND) != null ||
                pushIntent.getStringExtra(EXTRA_CUSTOM_SOUND) != null;
    }

    /**
     * Returns the color resource ID that was set in the intent.
     *
     * @param pushIntent The push intent.
     * @return The color id as a string, null if none was set.
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
