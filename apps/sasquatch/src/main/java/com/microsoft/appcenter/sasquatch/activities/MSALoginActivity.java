/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.AUTHORIZE_URL;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.CLIENT_ID_PARAM;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.REDIRECT_URI_PARAM;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.REDIRECT_URL;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.REFRESH_TOKEN;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.SCOPE;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.SIGN_OUT_URL;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.TOKEN_URL;
import static com.microsoft.appcenter.sasquatch.MSAAuthenticationProvider.USER_ID;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_AUTH_TYPE_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_REFRESH_TOKEN_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_REFRESH_TOKEN_SCOPE_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_TOKEN_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.sSharedPreferences;

public class MSALoginActivity extends AppCompatActivity {

    private static final String[] SCOPES_COMPACT = {"service::events.data.microsoft.com::MBI_SSL"};

    private static final String[] SCOPES_DELEGATE = {
            "wl.offline_access",
            "AsimovRome.Telemetry",
    };

    /**
     * HTTP Client to get tokens.
     */
    private static HttpClient sHttpClient;

    /**
     * Refresh token if logged in.
     */
    private String mRefreshToken;

    /**
     * Refresh token scope.
     */
    private String mRefreshTokenScope;

    /**
     * Web view.
     */
    private WebView mWebView;

    /**
     * Authentication provider type.
     */
    private AuthenticationProvider.Type mAuthType;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        /* Init UI. */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msa_login);

        Serializable rawAuthType = getIntent().getSerializableExtra(AuthenticationProvider.Type.class.getName());
        mAuthType = (AuthenticationProvider.Type) rawAuthType;

        /* Init API client only once. */
        if (sHttpClient == null) {
            sHttpClient = createHttpClient(this);
        }

        /* Configure web view. */
        mWebView = findViewById(R.id.web_view);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setJavaScriptEnabled(true);

        /* Show prompt or message that there will be no prompt to sign in. */
        String cookie = CookieManager.getInstance().getCookie(AUTHORIZE_URL);
        boolean compact = mAuthType == AuthenticationProvider.Type.MSA_COMPACT;
        if (compact && cookie != null && cookie.contains("MSPPre")) {
            mWebView.loadData(getString(R.string.signed_in_cookie), "text/plain", "UTF-8");
        } else {
            signIn(null);
        }
    }

    @SuppressWarnings("unused")
    public void signIn(View view) {
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                checkSignInCompletion(url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                failSignIn(errorCode, description);
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                failSignIn(error.getErrorCode(), error.getDescription());
            }
        });
        boolean compactTicket = mAuthType == AuthenticationProvider.Type.MSA_COMPACT;
        String responseType = compactTicket ? "token" : "code";
        String[] scopes = compactTicket ? SCOPES_COMPACT : SCOPES_DELEGATE;
        mWebView.loadUrl(AUTHORIZE_URL + REDIRECT_URI_PARAM + CLIENT_ID_PARAM + "&response_type=" + responseType +
                "&scope=" + TextUtils.join("+", scopes));
    }

    @SuppressWarnings("unused")
    public void signOut(View view) {
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.startsWith(REDIRECT_URL)) {
                    clearCookies();
                    Uri uri = Uri.parse(url);

                    sSharedPreferences.edit().remove(MSA_REFRESH_TOKEN_SCOPE_KEY).apply();
                    sSharedPreferences.edit().remove(MSA_REFRESH_TOKEN_KEY).apply();
                    sSharedPreferences.edit().remove(MSA_AUTH_TYPE_KEY).apply();
                    sSharedPreferences.edit().remove(MSA_TOKEN_KEY).apply();
                    String error = uri.getQueryParameter("error");
                    if (error != null) {
                        failSignOut(0, error);
                    } else {
                        signIn(null);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                clearCookies();
                failSignOut(errorCode, description);
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                clearCookies();
                failSignOut(error.getErrorCode(), error.getDescription());
            }
        });
        mWebView.loadUrl(SIGN_OUT_URL + REDIRECT_URI_PARAM + CLIENT_ID_PARAM);
    }

    private void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            android.webkit.CookieSyncManager cookieSyncManager = android.webkit.CookieSyncManager.createInstance(this);
            cookieSyncManager.startSync();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
    }

    /**
     * Display sign in error and exit screen.
     */
    private void failSignIn(int errorCode, CharSequence description) {
        Log.e(LOG_TAG, "Failed to sign in errorCode=" + errorCode + " description=" + description);
        Toast.makeText(this, R.string.sign_in_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Display sign out error and exit screen.
     */
    private void failSignOut(int errorCode, CharSequence description) {
        Log.e(LOG_TAG, "Failed to sign out errorCode=" + errorCode + " description=" + description);
        Toast.makeText(this, R.string.sign_out_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Check from mWebView view if sign in completed and process completion.
     */
    private void checkSignInCompletion(String url) {
        if (url.startsWith(REDIRECT_URL)) {
            Uri uri = Uri.parse(url);
            if (mAuthType == AuthenticationProvider.Type.MSA_COMPACT) {
                String fragment = uri.getFragment();
                if (fragment != null) {

                    /* Convert fragment to query string to process response. */
                    checkCompactSignInCompletion(Uri.parse(REDIRECT_URL + "?" + fragment));
                }
            } else {
                checkDelegateSignInCompletion(uri);
            }
        }
    }

    private void checkCompactSignInCompletion(Uri uri) {
        mRefreshToken = uri.getQueryParameter(REFRESH_TOKEN);
        mRefreshTokenScope = SCOPES_COMPACT[0];
        if (!TextUtils.isEmpty(mRefreshToken)) {
            registerAppCenterAuthentication(uri.getQueryParameter(USER_ID));
        } else {
            failSignIn(0, uri.getQueryParameter("error_description"));
        }
    }

    private void checkDelegateSignInCompletion(Uri uri) {
        String code = uri.getQueryParameter("code");
        if (TextUtils.isEmpty(code)) {
            failSignIn(0, "error=" + uri.getQueryParameter("error"));
        } else {
            getToken(code);
        }
    }

    private void registerAppCenterAuthentication(String userId) {
        sSharedPreferences.edit().putString(MSA_REFRESH_TOKEN_SCOPE_KEY, mRefreshTokenScope).apply();
        sSharedPreferences.edit().putString(MSA_REFRESH_TOKEN_KEY, mRefreshToken).apply();
        sSharedPreferences.edit().putInt(MSA_AUTH_TYPE_KEY, mAuthType.ordinal()).apply();
        sSharedPreferences.edit().putString(MSA_TOKEN_KEY, userId).apply();
        MSAAuthenticationProvider tokenProvider = MSAAuthenticationProvider.getInstance(mRefreshToken, mRefreshTokenScope, this);
        AuthenticationProvider provider = new AuthenticationProvider(mAuthType, userId, tokenProvider);
        AnalyticsTransmissionTarget.addAuthenticationProvider(provider);
        finish();
        Toast.makeText(this, R.string.signed_in, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get initial access token.
     */
    private void getToken(final String code) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DefaultHttpClient.CONTENT_TYPE_KEY, "application/x-www-form-urlencoded");
        sHttpClient.callAsync(TOKEN_URL,
                DefaultHttpClient.METHOD_POST,
                headers,
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        return REDIRECT_URI_PARAM +
                                CLIENT_ID_PARAM +
                                "&grant_type=authorization_code" +
                                "&code=" + code;
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                        AppCenterLog.verbose(AppCenter.LOG_TAG, "Calling " + url + "...");
                    }
                },
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(HttpResponse httpResponse) {
                        try {
                            JSONObject response = new JSONObject(httpResponse.getPayload());
                            String userId = response.getString(USER_ID);
                            mRefreshToken = response.getString(REFRESH_TOKEN);
                            mRefreshTokenScope = response.getString(SCOPE);
                            registerAppCenterAuthentication(userId);
                        } catch (JSONException e) {
                            onCallFailed(e);
                        }
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        handleCallFailure(e);
                    }
                });
    }

    private void handleCallFailure(Exception e) {
        if (e instanceof HttpException) {
            HttpException he = (HttpException) e;
            failSignIn(he.getHttpResponse().getStatusCode(), he.getHttpResponse().getPayload());
        } else {
            failSignIn(0, e.getMessage());
        }
    }
}
