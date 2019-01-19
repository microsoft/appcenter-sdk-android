package com.microsoft.appcenter.identity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.identity.client.PublicClientApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;

/**
 * Identity service.
 */
public class Identity extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Identity";

    /**
     * TAG used in logging for Identity.
     */
    private static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the identity group.
     */
    static final String IDENTITY_GROUP = "group_identity";

    private static final String CONFIG_URL = "https://mobilecentersdkdev.blob.core.windows.net/identity/%s.json";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Identity sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Application secret.
     */
    private String mAppSecret;

    private PublicClientApplication mAuthenticationClient;

    /**
     * Scope we need to use when acquiring user ID tokens.
     */
    private String mIdentityScope;


    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Identity getInstance() {
        if (sInstance == null) {
            sInstance = new Identity();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Identity service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings("unused")
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Identity service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings("unused")
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mAppSecret = appSecret;
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            if (mAppSecret != null) {
                downloadConfiguration();
            } else {
                AppCenterLog.error(LOG_TAG, "Identity needs to be started with an application secret.");
            }
        } else {
            //TODO: cancel tasks.
        }
    }

    @Override
    protected String getGroupName() {
        return IDENTITY_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    private void downloadConfiguration() {
        HttpClient httpClient = createHttpClient(mContext);
        Map<String, String> headers = new HashMap<>();
        // TODO add if-none-match with etag value
        //headers.put("If-None-Match", "\"0x8D67DA160EB33ED\"");
        String url = String.format(CONFIG_URL, mAppSecret);
        httpClient.callAsync(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

            @Override
            public String buildRequestBody() {
                return null;
            }

            @Override
            public void onBeforeCalling(URL url, Map<String, String> headers) {
                if (AppCenterLog.getLogLevel() <= VERBOSE) {
                    String urlString = url.toString().replaceAll(mAppSecret, HttpUtils.hideSecret(mAppSecret));
                    AppCenterLog.verbose(LOG_TAG, "Calling " + urlString + "...");
                    AppCenterLog.verbose(LOG_TAG, "Headers: " + headers);
                }
            }
        }, new ServiceCallback() {

            @Override
            public void onCallSucceeded(String payload, Map<String, String> headers) {
                processDownloadedConfig(payload, headers.get("ETag"));
            }

            @Override
            public void onCallFailed(Exception e) {
                AppCenterLog.error(LOG_TAG, "Cannot load identity configuration from the server.", e);
            }
        });
    }


    private void processDownloadedConfig(String payload, String eTag) {
        try {
            JSONObject configuration = new JSONObject(payload);
            String identityScope = configuration.getString("identity_scope");
            String clientId = configuration.getString("client_id");
            String authorityUrl = null;
            JSONArray authorities = configuration.getJSONArray("authorities");
            for (int i = 0; i < authorities.length(); i++) {
                JSONObject authority = authorities.getJSONObject(i);
                if (authority.optBoolean("default") && "B2C".equals(authority.getString("type"))) {
                    authorityUrl = authority.getString("authority_url");
                    break;
                }
            }
            if (authorityUrl != null) {
                mAuthenticationClient = new PublicClientApplication(mContext, clientId, authorityUrl);
                mIdentityScope = identityScope;
                AppCenterLog.info(LOG_TAG, "Identity service configured successfully.");
            } else {
                throw new IllegalStateException("Cannot find a b2c authority configured to be the default.");
            }
        } catch (JSONException | RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "The download configuration is invalid.", e);
        }
    }
}
