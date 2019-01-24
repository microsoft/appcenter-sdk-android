package com.microsoft.appcenter.identity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.identity.client.internal.controllers.MSALApiDispatcher;

public class IdentityAcquireTokenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcenter_identity_activity);
        Identity.getInstance().login(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MSALApiDispatcher.completeInteractive(requestCode, resultCode, data);
        finish();
    }
}
