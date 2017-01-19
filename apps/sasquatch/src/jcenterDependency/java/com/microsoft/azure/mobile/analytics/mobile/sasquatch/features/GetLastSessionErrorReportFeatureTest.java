package com.microsoft.azure.mobile.analytics.mobile.sasquatch.features;

import android.util.Log;

import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;

import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.LOG_TAG;

public class GetLastSessionErrorReportFeatureTest {

    public static void run() {
        ErrorReport lastSessionCrashReport = Crashes.getLastSessionCrashReport();
        if (lastSessionCrashReport != null) {
            Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", lastSessionCrashReport.getThrowable());
        }
    }
}
