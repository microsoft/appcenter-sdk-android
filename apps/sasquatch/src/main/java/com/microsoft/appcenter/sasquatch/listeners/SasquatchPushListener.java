/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.listeners;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.appcenter.push.PushNotification;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class SasquatchPushListener implements com.microsoft.appcenter.push.PushListener {

    @Override
    public void onPushNotificationReceived(Activity activity, PushNotification pushNotification) {
        String title = pushNotification.getTitle();
        String message = pushNotification.getMessage();
        Map<String, String> customData = pushNotification.getCustomData();
        Log.i(LOG_TAG, "Push received title=" + title + " message=" + message + " customData=" + customData + " activity=" + activity);
        if (message != null) {
            android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(activity);
            dialog.setTitle(title);
            dialog.setMessage(message);
            if (!customData.isEmpty()) {
                dialog.setMessage(message + "\n" + customData);
            }
            dialog.setPositiveButton(android.R.string.ok, null);
            dialog.show();
        } else {
            Toast.makeText(activity, String.format(activity.getString(R.string.push_toast), customData), Toast.LENGTH_LONG).show();
        }
    }
}
