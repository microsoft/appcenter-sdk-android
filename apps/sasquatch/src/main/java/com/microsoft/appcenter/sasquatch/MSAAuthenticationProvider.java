package com.microsoft.appcenter.sasquatch;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_REFRESH_TOKEN_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_USER_ID_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.sSharedPreferences;

public class MSAAuthenticationProvider implements AuthenticationProvider.TokenProvider {

    private static final String URL_PREFIX = "https://login.live.com/oauth20_";

    private static final String CLIENT_ID = "06181c2a-2403-437f-a490-9bcb06f85281";

    private static final String EXPIRES_IN = "expires_in";

    public static final String REDIRECT_URL = URL_PREFIX + "desktop.srf";

    public static final String AUTHORIZE_URL = URL_PREFIX + "authorize.srf?";

    public static final String TOKEN_URL = URL_PREFIX + "token.srf";

    public static final String SIGN_OUT_URL = URL_PREFIX + "logout.srf?";

    public static final String SCOPE = "scope";

    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String CLIENT_ID_PARAM = "&client_id=" + CLIENT_ID;

    public static final String REDIRECT_URI_PARAM;

    public static final String USER_ID = "user_id";

    static {
        try {
            REDIRECT_URI_PARAM = "redirect_uri=" + URLEncoder.encode(REDIRECT_URL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static MSAAuthenticationProvider instance;

    private String mRefreshToken;

    private String mRefreshTokenScope;

    private WeakReference<Activity> mActivity;
    
    private HttpClient mHttpClient;

    private MSAAuthenticationProvider(String refreshToken, String refreshTokenScope, Activity activity) {
        mRefreshToken = refreshToken;
        mRefreshTokenScope = refreshTokenScope;
        mActivity = new WeakReference<>(activity);
        mHttpClient = createHttpClient(activity);
    }

    public static MSAAuthenticationProvider getInstance(String refreshToken, String refreshTokenScope, Activity activity) {
        if (instance == null) {
            instance = new MSAAuthenticationProvider(refreshToken, refreshTokenScope, activity);
        } else {
            instance.mRefreshToken = refreshToken;
            instance.mActivity = new WeakReference<>(activity);
        }
        return instance;
    }

    @Override
    public void acquireToken(String ticketKey, final AuthenticationProvider.AuthenticationCallback callback) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DefaultHttpClient.CONTENT_TYPE_KEY, "application/x-www-form-urlencoded");
        mHttpClient.callAsync(TOKEN_URL,
                DefaultHttpClient.METHOD_POST,
                headers,
                new HttpClient.CallTemplate() {

                    @Override
                    public String buildRequestBody() {
                        return REDIRECT_URI_PARAM +
                                CLIENT_ID_PARAM +
                                "&grant_type=" + REFRESH_TOKEN +
                                "&" + REFRESH_TOKEN + "=" + mRefreshToken +
                                "&scope=" + mRefreshTokenScope;
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
                            String accessToken = response.getString("access_token");
                            String userId = response.getString(USER_ID);
                            long expiresIn = response.getLong(EXPIRES_IN) * 1000L;
                            Date expiryDate = new Date(System.currentTimeMillis() + expiresIn);
                            sSharedPreferences.edit().putString(MSA_REFRESH_TOKEN_KEY, mRefreshToken).apply();
                            sSharedPreferences.edit().putString(MSA_USER_ID_KEY, userId).apply();
                            callback.onAuthenticationResult(accessToken, expiryDate);
                        } catch (JSONException e) {
                            onCallFailed(e);
                        }
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        callback.onAuthenticationResult(null, null);
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

    /**
     * Display sign in error and exit screen.
     */
    private void failSignIn(int errorCode, CharSequence description) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        Log.e(LOG_TAG, "Failed to sign in errorCode=" + errorCode + " description=" + description);
        Toast.makeText(activity, R.string.sign_in_failed, Toast.LENGTH_SHORT).show();
        activity.finish();
    }
}
