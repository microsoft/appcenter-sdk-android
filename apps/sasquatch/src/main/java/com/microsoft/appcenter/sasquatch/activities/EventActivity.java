package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.os.Bundle;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;

import java.lang.reflect.Method;
import java.util.Map;

public class EventActivity extends LogActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* TODO remove reflection once API available in jCenter. */
        try {
            Method startFromLibrary = AppCenter.class.getMethod("startFromLibrary", Context.class, Class[].class);
            startFromLibrary.invoke(null, getApplication(), new Class[]{Analytics.class});
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        String target = getTransmissionTarget();
        if (target == null) {
            Analytics.trackEvent(name, properties);
        } else {
            Analytics.getTransmissionTarget(target).trackEvent(name, properties);
        }
    }
}
