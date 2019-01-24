package com.microsoft.appcenter.identity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

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
import static com.microsoft.appcenter.identity.Constants.AUTHORITIES;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_DEFAULT;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE_B2C;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_URL;
import static com.microsoft.appcenter.identity.Constants.CONFIG_URL;
import static com.microsoft.appcenter.identity.Constants.FILE_PATH;
import static com.microsoft.appcenter.identity.Constants.HEADER_E_TAG;
import static com.microsoft.appcenter.identity.Constants.HEADER_IF_NONE_MATCH;
import static com.microsoft.appcenter.identity.Constants.IDENTITY_GROUP;
import static com.microsoft.appcenter.identity.Constants.IDENTITY_SCOPE;
import static com.microsoft.appcenter.identity.Constants.LOG_TAG;
import static com.microsoft.appcenter.identity.Constants.PREFERENCE_E_TAG_KEY;
import static com.microsoft.appcenter.identity.Constants.SERVICE_NAME;

/**
 * Identity service.
 */
public class Identity extends AbstractAppCenterService {

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
     * HTTP client call, for cancellation.
     */
    private ServiceCall mGetConfigCall;

    /**
     * Current activity.
     */
    private Activity mActivity;

    private boolean mLoginDelayed;

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

    public static void login() {
        getInstance().instanceLogin();
    }

    @Override
    @WorkerThread
    public void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
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
    @WorkerThread
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            if (mAppSecret != null) {

                /* Download fresh configuration first instead of using cache directly. */
                downloadConfiguration();
            } else {
                AppCenterLog.error(LOG_TAG, "Identity needs to be started with an application secret.");
            }
        } else {
            if (mGetConfigCall != null) {
                mGetConfigCall.cancel();
                mGetConfigCall = null;
            }
            mAuthenticationClient = null;
            mIdentityScope = null;
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

    @Override
    public void onActivityResumed(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mActivity = null;
    }

    @WorkerThread
    private void downloadConfiguration() {

        /* Configure http call to download the configuration. Add ETag if we have a cached entry. */
        HttpClient httpClient = createHttpClient(mContext);
        Map<String, String> headers = new HashMap<>();
        String eTag = SharedPreferencesManager.getString(PREFERENCE_E_TAG_KEY);
        if (eTag != null) {
            headers.put(HEADER_IF_NONE_MATCH, eTag);
        }
        String url = String.format(CONFIG_URL, mAppSecret);
        mGetConfigCall = httpClient.callAsync(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

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
            public void onCallSucceeded(final String payload, final Map<String, String> headers) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        processDownloadedConfig(payload, headers.get(HEADER_E_TAG));
                    }
                });
            }

            @Override
            public void onCallFailed(final Exception e) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 304) {
                            loadConfigurationFromCache();
                        } else {
                            processDownloadError(e);
                        }
                    }
                });
            }
        });
    }

    @WorkerThread
    private void processDownloadedConfig(String payload, String eTag) {
        mGetConfigCall = null;
        saveConfigFile(payload, eTag);
        initAuthenticationClient(payload);
    }

    @WorkerThread
    private void processDownloadError(Exception e) {
        mGetConfigCall = null;
        AppCenterLog.error(LOG_TAG, "Cannot load identity configuration from the server.", e);
    }

    @WorkerThread
    private void loadConfigurationFromCache() {
        mGetConfigCall = null;
        AppCenterLog.info(LOG_TAG, "Identify configuration didn't change, will use cache.");
        boolean configurationSucceeded = false;
        String configurationPayload = FileManager.read(getConfigFile());
        if (configurationPayload != null) {
            configurationSucceeded = initAuthenticationClient(configurationPayload);
        }
        if (!configurationSucceeded) {
            downloadConfiguration();
        }
    }

    @WorkerThread
    private boolean initAuthenticationClient(String configurationPayload) {

        /* Parse configuration. */
        try {
            JSONObject configuration = new JSONObject(configurationPayload);
            String identityScope = configuration.getString(IDENTITY_SCOPE);
            String authorityUrl = null;
            JSONArray authorities = configuration.getJSONArray(AUTHORITIES);
            for (int i = 0; i < authorities.length(); i++) {
                JSONObject authority = authorities.getJSONObject(i);
                if (authority.optBoolean(AUTHORITY_DEFAULT) && AUTHORITY_TYPE_B2C.equals(authority.getString(AUTHORITY_TYPE))) {
                    authorityUrl = authority.getString(AUTHORITY_URL);
                    break;
                }
            }
            if (authorityUrl != null) {

                /* The remaining validation is done by the library. */
                mAuthenticationClient = new PublicClientApplication(mContext, getConfigFile());
                mIdentityScope = identityScope;
                AppCenterLog.info(LOG_TAG, "Identity service configured successfully.");
                return true;
            } else {
                throw new IllegalStateException("Cannot find a b2c authority configured to be the default.");
            }
        } catch (JSONException | RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "The configuration is invalid.", e);
            clearCache();
        }
        return false;
    }

    @NonNull
    private File getConfigFile() {
        return new File(mContext.getFilesDir(), FILE_PATH);
    }

    @WorkerThread
    private void saveConfigFile(String payload, String eTag) {
        File file = getConfigFile();
        FileManager.mkdir(file.getParent());
        try {
            FileManager.write(file, payload);
            SharedPreferencesManager.putString(PREFERENCE_E_TAG_KEY, eTag);
        } catch (IOException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to cache identity configuration.", e);
        }
        AppCenterLog.debug(LOG_TAG, "Identity configuration saved in cache.");
    }

    @WorkerThread
    private void clearCache() {
        SharedPreferencesManager.remove(PREFERENCE_E_TAG_KEY);
        FileManager.delete(getConfigFile());
    }

    private void instanceLogin() {
        post(new Runnable() {

            @Override
            public void run() {
                loginAsync();
            }
        });
    }

    @WorkerThread
    private void loginAsync() {
        final PublicClientApplication authenticationClient = mAuthenticationClient;
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (authenticationClient != null && mActivity != null) {
                    login(mActivity);
                } else {

                    // TODO handle that when both configuration and activity become available.
                    mLoginDelayed = true;
                }
            }
        });
    }

    @UiThread
    private void login(Activity activity) {
        mAuthenticationClient.acquireToken(activity, new String[]{mIdentityScope}, new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                AppCenterLog.info(LOG_TAG, "User login succeeded. id=" + authenticationResult.getIdToken());
                // TODO send id token (not access token) to ingestion.
            }

            @Override
            public void onError(MsalException exception) {
                AppCenterLog.error(LOG_TAG, "User login failed.", exception);
            }

            @Override
            public void onCancel() {
                AppCenterLog.warn(LOG_TAG, "User canceled login.");
            }
        });
    }
}
