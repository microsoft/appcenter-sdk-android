/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.util;

import android.app.Activity;

import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.sasquatch.R;

import java.util.ArrayList;
import java.util.List;

public class EventActivityUtil {

    public static List<AnalyticsTransmissionTarget> getAnalyticTransmissionTargetList(Activity activity) {
        List<AnalyticsTransmissionTarget> targets = new ArrayList<>();

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        String[] targetTokens = activity.getResources().getStringArray(R.array.target_id_values);
        targets.add(null);
        targets.add(Analytics.getTransmissionTarget(targetTokens[1]));
        for (int i = 2; i < targetTokens.length; i++) {
            String targetToken = targetTokens[i];
            AnalyticsTransmissionTarget target = targets.get(i - 1).getTransmissionTarget(targetToken);
            targets.add(target);
        }
        return targets;
    }
}
