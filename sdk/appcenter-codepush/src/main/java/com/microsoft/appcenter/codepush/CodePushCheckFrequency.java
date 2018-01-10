package com.microsoft.appcenter.codepush.enums;

import com.google.gson.annotations.SerializedName;

/**
 * A enum defining when an update check should happen. 
*/
public enum CodePushCheckFrequency {

    /** 
     * Update check happens on application start.
     */
    @SerializedName("0")
    ON_APP_START(0),

    /**
     * Update check happens every time the application enters the screen (resumes).
    */
    @SerializedName("1")
    ON_APP_RESUME(1),

    /**
     * Update check is performed manually when the developer finds necessary.
    */
    @SerializedName("2")
    MANUAL(2);

    private final int value;

    CodePushCheckFrequency(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return this.value;
    }
}

