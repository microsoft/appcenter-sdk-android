/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.distribute.DistributeConstants.EXTRA_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.EXTRA_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.EXTRA_TESTER_APP_UPDATE_SETUP_FAILED;
import static com.microsoft.appcenter.distribute.DistributeConstants.EXTRA_UPDATE_SETUP_FAILED;
import static com.microsoft.appcenter.distribute.DistributeConstants.EXTRA_UPDATE_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * Generic activity used for deep linking in distribute.
 */
public class DeepLinkActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        /* Check intent. */
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String distributionGroupId = intent.getStringExtra(EXTRA_DISTRIBUTION_GROUP_ID);
        String updateToken = intent.getStringExtra(EXTRA_UPDATE_TOKEN);
        String updateSetupFailed = intent.getStringExtra(EXTRA_UPDATE_SETUP_FAILED);
        String testerAppUpdateSetupFailed = intent.getStringExtra(EXTRA_TESTER_APP_UPDATE_SETUP_FAILED);
        AppCenterLog.debug(LOG_TAG, getLocalClassName() + ".getIntent()=" + intent);
        AppCenterLog.debug(LOG_TAG, "Intent requestId=" + requestId);
        AppCenterLog.debug(LOG_TAG, "Intent distributionGroupId=" + distributionGroupId);
        AppCenterLog.debug(LOG_TAG, "Intent updateToken passed=" + (updateToken != null));
        AppCenterLog.debug(LOG_TAG, "Intent updateSetupFailed passed=" + (updateSetupFailed != null));
        AppCenterLog.debug(LOG_TAG, "Intent testerAppUpdateSetupFailed passed=" + (testerAppUpdateSetupFailed != null));

        /* Store redirection parameters if both required values were passed. */
        if (requestId != null && distributionGroupId != null) {
            Distribute.getInstance().storeRedirectionParameters(requestId, distributionGroupId, updateToken);
        } else if (requestId != null && updateSetupFailed != null) {

            /* Otherwise just store error message to show update failure dialog in future. */
            Distribute.getInstance().storeUpdateSetupFailedParameter(requestId, updateSetupFailed);
        }

        /* If tester app update setup failed, store that info to later retry using the browser update setup */
        if (requestId != null && testerAppUpdateSetupFailed != null) {
            Distribute.getInstance().storeTesterAppUpdateSetupFailedParameter(requestId, testerAppUpdateSetupFailed);
        }

        /*
         * Resume app exactly where it was before with no activity duplicate, or starting the
         * launcher if application task finished or killed (equivalent to clicking from launcher
         * or activity history).
         *
         * The browser used in emulator don't set the NEW_TASK flag and when we receive the intent,
         * isTaskRoot returns false even after API level 19 while application task was empty,
         * none of the intent flags combination seems to do what we want in that case.
         *
         * So we restart the activity with the correct flag this time as Chrome would do
         * and retry the isTaskRoot code and it will work correctly the second time...
         *
         * Also tried various finish() moveTaskToBack(true or false) combinations with no luck,
         * only the following code seems to work.
         */
        finish();
        if (!((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == Intent.FLAG_ACTIVITY_NEW_TASK)) {
            AppCenterLog.debug(LOG_TAG, "Using restart work around to correctly resume app.");
            startActivity(intent.cloneFilter().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (isTaskRoot()) {
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntentForPackage != null) {
                startActivity(launchIntentForPackage);
            }
        }
    }
}
