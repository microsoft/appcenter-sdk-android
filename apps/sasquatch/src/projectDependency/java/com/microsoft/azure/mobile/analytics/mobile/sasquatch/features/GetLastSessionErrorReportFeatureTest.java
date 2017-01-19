package com.microsoft.azure.mobile.analytics.mobile.sasquatch.features;

import android.support.annotation.Nullable;
import android.util.Log;

import com.microsoft.azure.mobile.ResultCallback;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;

import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.LOG_TAG;

public class GetLastSessionErrorReportFeatureTest {

    public static void run() {
        Crashes.getLastSessionCrashReport(new ResultCallback<ErrorReport>() {

            @Override
            public void onResult(@Nullable ErrorReport data) {
                if (data != null) {
                    Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", data.getThrowable());
                }
            }
        });
    }
}
