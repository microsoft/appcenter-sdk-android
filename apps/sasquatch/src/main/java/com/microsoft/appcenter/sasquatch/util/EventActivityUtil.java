package com.microsoft.appcenter.sasquatch.util;

import android.app.Activity;

import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.sasquatch.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EventActivityUtil {

    public static List<AnalyticsTransmissionTarget> getAnalyticTransmissionTargetList(Activity activity) {
        List<AnalyticsTransmissionTarget> targets = new ArrayList<>();
        Method getTransmissionTargetMethod;
        try {
            getTransmissionTargetMethod = AnalyticsTransmissionTarget.class.getMethod("getTransmissionTarget", String.class);
        } catch (NoSuchMethodException e) {
            getTransmissionTargetMethod = null;
        }

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        String[] targetTokens = activity.getResources().getStringArray(R.array.target_id_values);
        targets.add(Analytics.getTransmissionTarget(activity.getResources().getString(R.string.target_id)));
        targets.add(Analytics.getTransmissionTarget(targetTokens[1]));
        for (int i = 2; i < targetTokens.length; i++) {
            String targetToken = targetTokens[i];
            AnalyticsTransmissionTarget target;
            if (getTransmissionTargetMethod == null) {
                target = Analytics.getTransmissionTarget(targetToken);
            } else {
                try {
                    target = (AnalyticsTransmissionTarget) getTransmissionTargetMethod.invoke(targets.get(i - 1), targetToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            targets.add(target);
        }
        return targets;
    }
}
