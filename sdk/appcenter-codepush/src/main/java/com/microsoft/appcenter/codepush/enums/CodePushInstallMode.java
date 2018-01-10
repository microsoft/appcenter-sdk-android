package com.microsoft.appcenter.codepush.enums;

import com.google.gson.annotations.SerializedName;

/**
 * A enum defining how and when the install of an update should happen.
 */
public enum CodePushInstallMode {

    /**
     * Right after the update is downloaded.
     */
    @SerializedName("0")
    IMMEDIATE(0),

    /**
     * The next time application is restarted (reopened).
     */
    @SerializedName("1")
    ON_NEXT_RESTART(1),

    /**
     * The next time application enters the screen (resumes).
     */
    @SerializedName("2")
    ON_NEXT_RESUME(2),

    /**
     * The next time application suspends.
     */
    @SerializedName("3")
    ON_NEXT_SUSPEND(3);

    private final int value;

    CodePushInstallMode(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return this.value;
    }
}