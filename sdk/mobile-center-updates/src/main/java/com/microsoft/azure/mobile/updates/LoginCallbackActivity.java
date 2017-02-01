package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import static com.microsoft.azure.mobile.updates.Updates.EXTRA_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.updates.Updates.LOG_TAG;

public class LoginCallbackActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        /*
         * Get update token from intent.
         * TODO protect intent: verifying signature with server public key in another field seems like a good way.
         * But it would not protect against spamming intents to cause app to use CPU to verify fake signatures.
         */
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String updateToken = intent.getStringExtra(EXTRA_UPDATE_TOKEN);
        MobileCenterLog.debug(LOG_TAG, getLocalClassName() + ".getIntent()=" + intent);
        MobileCenterLog.verbose(LOG_TAG, getLocalClassName() + ".getIntent()#S.update_token=" + updateToken);

        /* Store update token. */
        if (updateToken != null) {
            Updates.getInstance().storeUpdateToken(updateToken);
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
            MobileCenterLog.debug(LOG_TAG, "Using restart work around to correctly resume app.");
            startActivity(intent.cloneFilter().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (isTaskRoot()) {
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntentForPackage != null) {
                startActivity(launchIntentForPackage);
            }
        }
    }
}
