package com.microsoft.appcenter.codepush.enums;
 
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Constants defining when an update check should happen.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
    CodePushCheckFrequency.ON_APP_START,
    CodePushCheckFrequency.ON_APP_RESUME,
    CodePushCheckFrequency.MANUAL
})
public @interface CodePushCheckFrequency {

    /**
     * Update check happens on application start.
     */
    int ON_APP_START = 0;

    /**
     * Update check happens every time the application enters the screen (resumes).
     */
    int ON_APP_RESUME = 1;

    /**
     * Update check is performed manually when the developer finds necessary.
     */
    int MANUAL = 2;
}