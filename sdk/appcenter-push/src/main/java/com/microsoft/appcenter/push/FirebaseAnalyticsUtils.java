package com.microsoft.appcenter.push;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

class FirebaseAnalyticsUtils {

    @SuppressWarnings("MissingPermission")
    static void setEnabled(Context context, boolean enabled) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled);
    }
}
