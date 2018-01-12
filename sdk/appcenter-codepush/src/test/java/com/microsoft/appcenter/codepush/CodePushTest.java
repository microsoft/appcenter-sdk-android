package com.microsoft.appcenter.codepush;

import com.microsoft.appcenter.codepush.enums.CodePushCheckFrequency;
import com.microsoft.appcenter.codepush.enums.CodePushDeploymentStatus;
import com.microsoft.appcenter.codepush.enums.CodePushInstallMode;
import com.microsoft.appcenter.codepush.enums.CodePushSyncStatus;
import com.microsoft.appcenter.codepush.enums.CodePushUpdateState;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CodePushTest {

    @Test
    public void enumsTest() throws Exception {
        CodePushCheckFrequency codePushCheckFrequency = CodePushCheckFrequency.MANUAL;
        int checkFrequencyValue = codePushCheckFrequency.getValue();
        assertEquals(checkFrequencyValue, 2);

        CodePushDeploymentStatus codePushDeploymentStatus = CodePushDeploymentStatus.SUCCEEDED;
        String deploymentStatusValue = codePushDeploymentStatus.getValue();
        assertEquals(deploymentStatusValue, "DeploymentSucceeded");

        CodePushInstallMode codePushInstallMode = CodePushInstallMode.IMMEDIATE;
        int installModeValue = codePushInstallMode.getValue();
        assertEquals(installModeValue, 0);

        CodePushSyncStatus codePushSyncStatus = CodePushSyncStatus.AWAITING_USER_ACTION;
        int syncStatusValue = codePushSyncStatus.getValue();
        assertEquals(syncStatusValue, 6);

        CodePushUpdateState codePushUpdateState = CodePushUpdateState.LATEST;
        int updateStateValue = codePushUpdateState.getValue();
        assertEquals(updateStateValue, 2);
    }
}