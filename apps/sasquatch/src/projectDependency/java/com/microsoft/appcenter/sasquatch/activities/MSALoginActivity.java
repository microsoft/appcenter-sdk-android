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

import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

/**
 * TODO move to main source folder after SDK release (and delete jCenter version).
 */
public class MSALoginActivity extends AppCompatActivity {

    private static final String[] SCOPES = {
            "wl.offline_access",
            "ccs.ReadWrite",
            "dds.read",
            "dds.register",
            "wns.connect",
            "asimovrome.telemetry",
            "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp",
    };

    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";

    private static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";

    private static final String CODE = "code";

    private static final String CLIENT_ID = "06181c2a-2403-437f-a490-9bcb06f85281";

    /**
     * HTTP Client to get tokens.
     */
    private HttpClient mHttpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msa_login);
        HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
        NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(this);
        mHttpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        signIn();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void signIn() {
        String signInUrl = AUTHORIZE_URL + "?redirect_uri=" + REDIRECT_URL + "&response_type=code&client_id=" + CLIENT_ID +
                "&scope=" + TextUtils.join("+", SCOPES);
        WebView web = findViewById(R.id.web_view);
        web.setWebChromeClient(new WebChromeClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.loadUrl(signInUrl);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            web.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    checkLoginUrl(url);
                }

                @Override
                @TargetApi(23)
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    fail(error.getErrorCode(), error.getDescription());
                }
            });
        } else {
            web.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    checkLoginUrl(url);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    fail(errorCode, description);
                }
            });
        }
    }

    public void reset(View view) {
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
        signIn();
    }

    private void fail(int errorCode, CharSequence description) {
        Log.e(LOG_TAG, "Failed to login errorCode=" + errorCode + " message=" + description);
        Toast.makeText(this, R.string.failed_login, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void checkLoginUrl(String url) {
        if (url.startsWith(REDIRECT_URL)) {
            Uri uri = Uri.parse(url);
            String code = uri.getQueryParameter(CODE);
            if (TextUtils.isEmpty(code)) {
                fail(0, "error=" + uri.getQueryParameter("error"));
            } else {
                getToken(code);
            }
        }
    }

    private void getToken(final String code) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DefaultHttpClient.CONTENT_TYPE_KEY, "application/x-www-form-urlencoded");
        mHttpClient.callAsync("https://login.live.com/oauth20_token.srf",
                DefaultHttpClient.METHOD_POST,
                headers,
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        return "client_id=" + CLIENT_ID +
                                "&grant_type=authorization_code" +
                                "&redirect_uri=" + REDIRECT_URL +
                                "&code=" + code;
                    }

                    @Override
                    public void onBeforeCalling(URL url, Map<String, String> headers) {
                    }
                },
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload) {
                        Log.i(LOG_TAG, payload);
                        try {
                            JSONObject response = new JSONObject(payload);
                            String userId = response.getString("user_id");
                            String accessToken = response.getString("access_token");
                            long expiresIn = response.getLong("expires_in") * 1000L;
                            Date expiresAt = new Date(System.currentTimeMillis() + expiresIn);
                            setAppCenterToken(userId, accessToken, expiresAt);
                        } catch (JSONException e) {
                            onCallFailed(e);
                        }
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        if (e instanceof HttpException) {
                            HttpException he = (HttpException) e;
                            fail(he.getStatusCode(), he.getPayload());
                        } else {
                            fail(0, e.getMessage());
                        }
                    }
                });
    }

    private void setAppCenterToken(String userId, final String accessToken, final Date expiresAt) {
        AuthenticationProvider.TokenProvider tokenProvider = new AuthenticationProvider.TokenProvider() {

            @Override
            public void getToken(String ticketKey, AuthenticationProvider.AuthenticationCallback callback) {

                /* Initial token. TODO handle refresh when we get called a second time. */
                callback.onAuthenticationResult(accessToken, expiresAt);
            }
        };
        AuthenticationProvider provider = new AuthenticationProvider(MSA, userId, tokenProvider);
        AnalyticsTransmissionTarget.addAuthenticationProvider(provider);
        finish();
    }
}
