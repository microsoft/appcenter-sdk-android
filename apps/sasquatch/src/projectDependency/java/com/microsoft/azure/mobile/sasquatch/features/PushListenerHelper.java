package com.microsoft.azure.mobile.sasquatch.features;

import com.microsoft.azure.mobile.push.Push;
import com.microsoft.azure.mobile.push.PushListener;
import com.microsoft.azure.mobile.push.PushNotification;
import com.microsoft.azure.mobile.sasquatch.activities.MainActivity;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

public class PushListenerHelper {

    public static void setup() {
        Push.setListener(new PushListener() {

            @Override
            public void onPushNotificationReceived(PushNotification pushNotification) {
                MobileCenterLog.info(MainActivity.LOG_TAG, "Push received title=" + pushNotification.getTitle() + " message=" + pushNotification.getMessage() + " customData=" + pushNotification.getCustomData() + " activity=" + pushNotification.getActivity().get());
            }
        });
    }
}
