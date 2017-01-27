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
            Updates.getInstance().storeUpdateToken(this, updateToken);
        }

        /* Resume app to avoid staying on browser if no application task. */
        if (isTaskRoot()) {
            startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
        }
        finish();
    }
}
