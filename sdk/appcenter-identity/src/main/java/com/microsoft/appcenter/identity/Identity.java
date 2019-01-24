package com.microsoft.appcenter.identity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.PublicClientApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
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
    private static final String IDENTITY_GROUP = "group_identity";

    private static final String CONFIG_URL = "https://mobilecentersdkdev.blob.core.windows.net/identity/%s.json";

    private static final String FILE_PATH = "appcenter/identity/config.json";

    private static final String PREFERENCE_ETAG_KEY = SERVICE_NAME + ".configFileETag";

    private static final String HEADER_E_TAG = "ETag";

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";

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

    /**
     * Authentication client.
     */
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

            /* Make module no-op if running on API level < 21, TODO this check can be removed when next msal lib released. */
            int minApiLevel = Build.VERSION_CODES.LOLLIPOP;
            if (Build.VERSION.SDK_INT <= minApiLevel) {
                AppCenterLog.error(LOG_TAG, "Identity requires API level " + minApiLevel);
                return;
            }
            if (mAppSecret != null) {

                /* Download fresh configuration first instead of using cache directly. */
                downloadConfiguration();
            } else {
                AppCenterLog.error(LOG_TAG, "Identity needs to be started with an application secret.");
            }
        } else {
            //TODO: cancel tasks.
            clearCache();
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

        /* Configure http call to download the configuration. Add ETag if we have a cached entry. */
        HttpClient httpClient = createHttpClient(mContext);
        Map<String, String> headers = new HashMap<>();
        String eTag = SharedPreferencesManager.getString(PREFERENCE_ETAG_KEY);
        if (eTag != null) {
            headers.put(HEADER_IF_NONE_MATCH, eTag);
        }
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
                processDownloadedConfig(payload, headers.get(HEADER_E_TAG));
            }

            @Override
            public void onCallFailed(Exception e) {
                if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 304) {
                    loadConfigurationFromCache();
                } else {
                    AppCenterLog.error(LOG_TAG, "Cannot load identity configuration from the server.", e);
                }
            }
        });
    }

    private synchronized void processDownloadedConfig(String payload, String eTag) {
        if (initAuthenticationClient(payload)) {
            saveConfigFile(payload, eTag);
        }
    }

    private synchronized void loadConfigurationFromCache() {
        AppCenterLog.info(LOG_TAG, "Identify configuration didn't change, will use cache.");
        boolean configurationSucceeded = false;
        String configurationPayload = FileManager.read(getConfigFile());
        if (configurationPayload != null) {
            configurationSucceeded = initAuthenticationClient(configurationPayload);
        }
        if (!configurationSucceeded) {
            clearCache();
            downloadConfiguration();
        }
    }

    private synchronized boolean initAuthenticationClient(String configurationPayload) {
        try {
            JSONObject configuration = new JSONObject(configurationPayload);
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

                // TODO use file to allow redirect_uri to be passed, need next msal version.
                mAuthenticationClient = new PublicClientApplication(mContext, clientId, authorityUrl);
                mIdentityScope = identityScope;
            } else {
                throw new IllegalStateException("Cannot find a b2c authority configured to be the default.");
            }
            AppCenterLog.info(LOG_TAG, "Identity service configured successfully.");
            return true;
        } catch (JSONException | RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "The configuration is invalid.", e);
        }
        return false;
    }

    @NonNull
    private File getConfigFile() {
        return new File(mContext.getFilesDir(), FILE_PATH);
    }

    private void saveConfigFile(String payload, String eTag) {
        File file = getConfigFile();
        FileManager.mkdir(file.getParent());
        try {
            FileManager.write(file, payload);
            SharedPreferencesManager.putString(PREFERENCE_ETAG_KEY, eTag);
        } catch (IOException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to cache identity configuration.", e);
        }
        AppCenterLog.debug(LOG_TAG, "Identity configuration saved in cache.");
    }

    private void clearCache() {
        SharedPreferencesManager.remove(PREFERENCE_ETAG_KEY);
        FileManager.delete(getConfigFile());
    }
}
