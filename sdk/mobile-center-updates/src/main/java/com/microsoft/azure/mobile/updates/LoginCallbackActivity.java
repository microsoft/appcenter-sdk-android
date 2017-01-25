package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import static com.microsoft.azure.mobile.updates.Updates.LOG_TAG;

public class LoginCallbackActivity extends Activity {

    private static final String EXTRA_COOKIE = "cookie";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String cookie = intent.getStringExtra(EXTRA_COOKIE);
        MobileCenterLog.debug(LOG_TAG, "LoginCallbackActivity.getIntent()=" + intent);
        MobileCenterLog.verbose(LOG_TAG, "LoginCallbackActivity.getIntent()#S.cookie=" + cookie);
        if (isTaskRoot()) {
            startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
        }
        finish();
    }
}
