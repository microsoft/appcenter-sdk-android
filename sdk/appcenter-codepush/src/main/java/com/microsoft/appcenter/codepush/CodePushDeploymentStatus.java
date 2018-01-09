package com.microsoft.codepush.react.enums;

import com.google.gson.annotations.SerializedName;

/**
 * A enum defining the deployment status.
 */
public enum CodePushDeploymentStatus {

    /**
     * Deployment process have been performed successfully.
     */
    @SerializedName("DeploymentSucceeded")
    SUCCEEDED("DeploymentSucceeded"),

    /**
     * Deployment process have been performed with errors.
     */
    @SerializedName("DeploymentFailed")
    FAILED("DeploymentFailed");

    private final String value;
    CodePushDeploymentStatus(String value) {
        this.value = value;
    }
    public String getValue() {
        return this.value;
    }
}