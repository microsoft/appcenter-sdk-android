package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
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
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

/**
 * TODO move to main source folder after SDK release (and delete jCenter version).
 */
public class MSALoginActivity extends AppCompatActivity {

    private static final String URL_PREFIX = "https://login.live.com/oauth20_";

    private static final String REDIRECT_URL = URL_PREFIX + "desktop.srf";

    private static final String AUTHORIZE_URL = URL_PREFIX + "authorize.srf?";

    private static final String TOKEN_URL = URL_PREFIX + "token.srf";

    private static final String SIGN_OUT_URL = URL_PREFIX + "logout.srf?";

    private static final String CLIENT_ID = "000000004C1D3F6C";

    private static final String SCOPE = "service::events.data.microsoft.com::MBI_SSL";

    private static final String REFRESH_TOKEN = "refresh_token";

    private static final String CLIENT_ID_PARAM = "&client_id=" + CLIENT_ID;

    private static final String SCOPE_PARAM = "&scope=" + SCOPE;

    private static String REDIRECT_URI_PARAM;

    static {
        try {
            REDIRECT_URI_PARAM = "redirect_uri=" + URLEncoder.encode(REDIRECT_URL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * HTTP Client to get tokens.
     */
    private static HttpClient sHttpClient;

    /**
     * Refresh token if logged in.
     */
    private static String sRefreshToken;

    /**
     * Web view.
     */
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        /* Init UI. */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msa_login);

        /* Init API client only once. */
        if (sHttpClient == null) {
            HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
            NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(this);
            sHttpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        }

        /* Configure web view. */
        mWebView = findViewById(R.id.web_view);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setJavaScriptEnabled(true);

        /* Show prompt or message that there will be no prompt to sign in. */
        String cookie = CookieManager.getInstance().getCookie(AUTHORIZE_URL);
        if (cookie.contains("MSPPre")) {
            mWebView.loadData(getString(R.string.signed_in_cookie), "text/plain", "UFT-8");
        } else {
            signIn(null);
        }
    }

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
        mWebView.loadUrl(AUTHORIZE_URL + REDIRECT_URI_PARAM + CLIENT_ID_PARAM + "&response_type=token" +
                SCOPE_PARAM);
    }

    public void signOut(View view) {
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.startsWith(REDIRECT_URL)) {
                    clearCookies();
                    Uri uri = Uri.parse(url);
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
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
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
            String fragment = uri.getFragment();
            if (fragment != null) {

                /* Convert fragment to query string to process response. */
                checkSignInCompletion(Uri.parse(REDIRECT_URL + "?" + fragment));
            }
        }
    }

    private void checkSignInCompletion(Uri uri) {
        sRefreshToken = uri.getQueryParameter(REFRESH_TOKEN);
        if (!TextUtils.isEmpty(sRefreshToken)) {
            registerAppCenterAuthentication(uri.getQueryParameter("user_id"));
        } else {
            failSignIn(0, uri.getQueryParameter("error_description"));
        }
    }

    private void registerAppCenterAuthentication(String userId) {
        AuthenticationProvider.TokenProvider tokenProvider = new AuthenticationProvider.TokenProvider() {

            @Override
            public void getToken(String ticketKey, AuthenticationProvider.AuthenticationCallback callback) {

                /* Refresh token, doing that even on first time to test the refresh code without having to wait 1 hour. */
                refreshToken(callback);
            }
        };
        AuthenticationProvider provider = new AuthenticationProvider(MSA, userId, tokenProvider);
        AnalyticsTransmissionTarget.addAuthenticationProvider(provider);
        finish();
        Toast.makeText(this, R.string.signed_in, Toast.LENGTH_SHORT).show();
    }

    private void refreshToken(final AuthenticationProvider.AuthenticationCallback callback) {
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
                                "&grant_type=" + REFRESH_TOKEN +
                                "&" + REFRESH_TOKEN + "=" + sRefreshToken +
                                SCOPE_PARAM;
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                        AppCenterLog.verbose(AppCenter.LOG_TAG, "Calling " + url + "...");
                    }
                },
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload) {
                        try {
                            JSONObject response = new JSONObject(payload);
                            String accessToken = response.getString("access_token");
                            long expiresIn = response.getLong("expires_in") * 1000L;
                            Date expiresAt = new Date(System.currentTimeMillis() + expiresIn);
                            callback.onAuthenticationResult("p:" + accessToken, expiresAt);
                        } catch (JSONException e) {
                            onCallFailed(e);
                        }
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        callback.onAuthenticationResult(null, null);
                        if (e instanceof HttpException) {
                            HttpException he = (HttpException) e;
                            failSignIn(he.getStatusCode(), he.getPayload());
                        } else {
                            failSignIn(0, e.getMessage());
                        }
                    }
                });
    }
}
