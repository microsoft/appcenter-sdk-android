package com.microsoft.azure.mobile.sasquatch.features;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.microsoft.azure.mobile.push.Push;
import com.microsoft.azure.mobile.push.PushListener;
import com.microsoft.azure.mobile.push.PushNotification;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.activities.MainActivity;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Map;

public class PushListenerHelper {

    public static void setup() {
        Push.setListener(new PushListener() {

            @Override
            public void onPushNotificationReceived(Activity activity, PushNotification pushNotification) {
                String title = pushNotification.getTitle();
                String message = pushNotification.getMessage();
                Map<String, String> customData = pushNotification.getCustomData();
                MobileCenterLog.info(MainActivity.LOG_TAG, "Push received title=" + title + " message=" + message + " customData=" + customData + " activity=" + activity);
                if (message != null) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
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
        });
    }
}
