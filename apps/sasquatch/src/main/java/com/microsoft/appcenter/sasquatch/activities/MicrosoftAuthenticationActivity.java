package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;

public class MicrosoftAuthenticationActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MicrosoftAuthentication";
    private static final String CLIENT_ID = "e5cf4c95-3ccb-4579-a978-ced840d7d5af";
    private static final String[] SCOPES = new String[] { "User.Read" };

    private PublicClientApplication mApplication;
    private AuthenticationResult mAuthentication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msa_auth);

        mApplication = new PublicClientApplication(this.getApplicationContext(), CLIENT_ID);
    }

    public void onLoginClick(View view) {
        mApplication.acquireToken(this, SCOPES, getAuthenticationCallback());
    }

    public void onRefreshClick(View view) {
        if (mAuthentication == null) {
            return;
        }
        mApplication.acquireTokenSilentAsync(SCOPES, mAuthentication.getUser(), null, true, getAuthenticationCallback());
    }

    public void onLogoutClick(View view) {
        mAuthentication = null;
        onUpdateAccessToken(null);
    }

    private void onUpdateAccessToken(String accessToken) {
        Log.i(LOG_TAG, "AccessToken: " + accessToken);
        // TODO
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    private AuthenticationCallback getAuthenticationCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                mAuthentication = authenticationResult;
                onUpdateAccessToken(authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                Log.e(LOG_TAG, exception.getMessage());
                if (exception instanceof MsalUiRequiredException) {

                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    onLoginClick(null);
                }
            }

            @Override
            public void onCancel() {
            }
        };
    }
}

