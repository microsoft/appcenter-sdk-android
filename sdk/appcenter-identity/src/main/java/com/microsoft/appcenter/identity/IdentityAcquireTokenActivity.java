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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }

        /*
         * Call identity back to launch the browser with this activity as source.
         * When we have more methods than just login,
         * we will need intent parameters to know how to call back.
         */
        Identity.getInstance().login(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MSALApiDispatcher.completeInteractive(requestCode, resultCode, data);
        finish();
    }
}
