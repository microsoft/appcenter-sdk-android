package com.microsoft.appcenter.identity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.identity.common.internal.controllers.ApiDispatcher;

public class IdentityAcquireTokenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        /*
         * TODO call via PublicClientApplication.handleInteractive instead of using internal API.
         * Was already renamed once. Drawback of PublicClientApplication is that it's not static.
         */
        ApiDispatcher.completeInteractive(requestCode, resultCode, data);
        finish();
    }
}
