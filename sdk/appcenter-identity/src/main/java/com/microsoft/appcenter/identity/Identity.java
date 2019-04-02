/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.AbstractTokenContextListener;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.identity.Constants.AUTHORITIES;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_DEFAULT;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE_B2C;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_URL;
import static com.microsoft.appcenter.identity.Constants.CONFIG_URL_FORMAT;
import static com.microsoft.appcenter.identity.Constants.DEFAULT_CONFIG_URL;
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
     * Current config base URL.
     */
    private String mConfigUrl = DEFAULT_CONFIG_URL;

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
     * Authority url for the authentication client.
     */
    private String mAuthorityUrl;

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

    /**
     * Not null if sign-in is pending.
     */
    private DefaultAppCenterFuture<SignInResult> mPendingSignInFuture;

    private AuthTokenContext.Listener mAuthTokenContextListener = new AbstractTokenContextListener() {

        @Override
        public void onTokenRequiresRefresh(String homeAccountId) {
            IAccount account = retrieveAccount(homeAccountId);
            if (account != null) {
                silentSignIn(account);
            } else {
                AppCenterLog.info(LOG_TAG, "Account is changed, reset to anonymous sending.");
                AuthTokenContext.getInstance().setAuthToken(null, null, null);
            }
        }
    };

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
     * Change the remote configuration base URL.
     *
     * @param configUrl configuration base URL.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    // TODO Remove warning suppress after release.
    public static void setConfigUrl(String configUrl) {
        getInstance().setInstanceConfigUrl(configUrl);
    }

    /**
     * Check whether Identity service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings("WeakerAccess") // TODO Remove warning suppress after release.
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Identity service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings("WeakerAccess") // TODO Remove warning suppress after release.
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Sign in to get user information.
     *
     * @return future with the result of the asynchronous sign-in operation.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning when JCenter published and demo updated
    public static AppCenterFuture<SignInResult> signIn() {
        return getInstance().instanceSignIn();
    }

    /**
     * Sign out user and invalidate a user's token.
     */
    @SuppressWarnings("WeakerAccess")
    // TODO remove warning once jCenter published and reflection removed in test app
    public static void signOut() {
        getInstance().instanceSignOut();
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mAppSecret = appSecret;

        /* The auth token from the previous launch is required. */
        AuthTokenContext.getInstance().doNotResetAuthAfterStart();
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
            AuthTokenContext.getInstance().addListener(mAuthTokenContextListener);

            /* Load cached configuration in case APIs are called early. */
            loadConfigurationFromCache();

            /* Download the latest configuration in background. */
            downloadConfiguration();
        } else {
            AuthTokenContext.getInstance().removeListener(mAuthTokenContextListener);
            if (mGetConfigCall != null) {
                mGetConfigCall.cancel();
                mGetConfigCall = null;
            }
            mAuthenticationClient = null;
            mIdentityScope = null;
            completeSignIn(null, new IllegalStateException("Identity is disabled."));
            clearCache();
            removeTokenAndAccount();
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
    public synchronized void onActivityResumed(Activity activity) {
        mActivity = activity;
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mActivity = null;
    }

    /**
     * Implements {@link #setConfigUrl(String)} at instance level.
     */
    private synchronized void setInstanceConfigUrl(String configUrl) {
        mConfigUrl = configUrl;
    }

    private synchronized void removeTokenAndAccount() {
        AuthTokenContext authTokenContext = AuthTokenContext.getInstance();
        removeAccount(authTokenContext.getHomeAccountId());
        authTokenContext.setAuthToken(null, null, null);
    }

    private synchronized void downloadConfiguration() {

        /* Configure http call to download the configuration. Add ETag if we have a cached entry. */
        HttpClient httpClient = createHttpClient(mContext);
        Map<String, String> headers = new HashMap<>();
        String eTag = SharedPreferencesManager.getString(PREFERENCE_E_TAG_KEY);
        if (eTag != null) {
            headers.put(HEADER_IF_NONE_MATCH, eTag);
        }
        String url = String.format(CONFIG_URL_FORMAT, mConfigUrl, mAppSecret);
        mGetConfigCall = httpClient.callAsync(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

            @Override
            public String buildRequestBody() {
                return null;
            }

            @Override
            public void onBeforeCalling(URL url, Map<String, String> headers) {
                if (AppCenter.getLogLevel() <= VERBOSE) {
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
                            processDownloadNotModified();
                        } else {
                            processDownloadError(e);
                        }
                    }
                });
            }
        });
    }

    @WorkerThread
    private synchronized void processDownloadedConfig(String payload, String eTag) {
        mGetConfigCall = null;
        saveConfigFile(payload, eTag);
        AppCenterLog.info(LOG_TAG, "Configure identity from downloaded configuration.");
        initAuthenticationClient(payload);
    }


    private synchronized void processDownloadNotModified() {
        mGetConfigCall = null;
        AppCenterLog.info(LOG_TAG, "Identity configuration didn't change.");
    }

    @WorkerThread
    private void loadConfigurationFromCache() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            AppCenterLog.info(LOG_TAG, "Configure identity from cached configuration.");
            initAuthenticationClient(FileManager.read(configFile));
        }
    }

    private synchronized void processDownloadError(Exception e) {
        mGetConfigCall = null;
        AppCenterLog.error(LOG_TAG, "Cannot load identity configuration from the server.", e);
    }

    @WorkerThread
    private synchronized void initAuthenticationClient(String configurationPayload) {

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
                mAuthorityUrl = authorityUrl;
                mIdentityScope = identityScope;
                AppCenterLog.info(LOG_TAG, "Identity service configured successfully.");
            } else {
                throw new IllegalStateException("Cannot find a b2c authority configured to be the default.");
            }
        } catch (JSONException | RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "The configuration is invalid.", e);
            clearCache();
        }
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
            AppCenterLog.debug(LOG_TAG, "Identity configuration saved in cache.");
        } catch (IOException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to cache identity configuration.", e);
        }
    }

    @WorkerThread
    private void clearCache() {
        SharedPreferencesManager.remove(PREFERENCE_E_TAG_KEY);
        FileManager.delete(getConfigFile());
        AppCenterLog.debug(LOG_TAG, "Identity configuration cache cleared.");
    }

    private synchronized AppCenterFuture<SignInResult> instanceSignIn() {
        final DefaultAppCenterFuture<SignInResult> future = new DefaultAppCenterFuture<>();
        if (mPendingSignInFuture != null) {
            future.complete(new SignInResult(null, new IllegalStateException("signIn already in progress.")));
            return future;
        }
        mPendingSignInFuture = future;
        Runnable disabledRunnable = new Runnable() {

            @Override
            public void run() {
                completeSignIn(null, new IllegalStateException("Identity is disabled."));
            }
        };
        post(new Runnable() {

            @Override
            public void run() {
                selectSignInTypeAndSignIn();
            }
        }, disabledRunnable, disabledRunnable);
        return future;
    }

    private void instanceSignOut() {
        post(new Runnable() {

            @Override
            public void run() {
                AuthTokenContext authTokenContext = AuthTokenContext.getInstance();
                if (authTokenContext.getAuthToken() == null) {
                    AppCenterLog.warn(LOG_TAG, "Cannot sign out because a user has not signed in.");
                    return;
                }
                removeTokenAndAccount();
                AppCenterLog.info(LOG_TAG, "User sign-out succeeded.");
            }
        });
    }

    private void removeAccount(String homeAccountIdentifier) {
        if (mAuthenticationClient == null) {
            return;
        }
        IAccount account = retrieveAccount(homeAccountIdentifier);
        if (account != null) {
            boolean result = mAuthenticationClient.removeAccount(account);
            AppCenterLog.debug(LOG_TAG, String.format("Remove account success=%s", result));
        }
    }

    @WorkerThread
    private IAccount retrieveAccount(String id) {
        if (id == null) {
            AppCenterLog.debug(LOG_TAG, "Cannot retrieve account: user id null.");
            return null;
        }
        IAccount account = mAuthenticationClient.getAccount(id, mAuthorityUrl);
        if (account == null) {
            AppCenterLog.warn(LOG_TAG, String.format("Cannot retrieve account: account id is null or missing: %s.", id));
        }
        return account;
    }

    @WorkerThread
    private synchronized void selectSignInTypeAndSignIn() {
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            completeSignIn(null, new NetworkErrorException("Sign-in failed. No internet connection."));
            return;
        }
        if (mAuthenticationClient == null) {
            completeSignIn(null, new IllegalStateException("signIn is called while it's not configured."));
            return;
        }
        AuthTokenContext authTokenContext = AuthTokenContext.getInstance();
        IAccount account = retrieveAccount(authTokenContext.getHomeAccountId());
        if (account != null) {
            silentSignIn(account);
        } else {
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    signInInteractively();
                }
            });
        }
    }

    @UiThread
    private synchronized void signInInteractively() {
        if (mAuthenticationClient != null && mActivity != null) {
            AppCenterLog.info(LOG_TAG, "Signing in using browser.");
            mAuthenticationClient.acquireToken(mActivity, new String[]{mIdentityScope}, new AuthenticationCallback() {

                @Override
                public void onSuccess(IAuthenticationResult authenticationResult) {
                    handleSignInSuccess(authenticationResult);
                }

                @Override
                public void onError(MsalException exception) {
                    handleSignInError(exception);
                }

                @Override
                public void onCancel() {
                    handleSignInCancellation();
                }
            });
        } else {
            completeSignIn(null, new IllegalStateException("signIn is called while it's not configured or not in the foreground."));
        }
    }

    private synchronized void silentSignIn(@NonNull IAccount account) {
        AppCenterLog.info(LOG_TAG, "Sign in silently in the background.");
        mAuthenticationClient.acquireTokenSilentAsync(new String[]{mIdentityScope}, account, null, true, new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                handleSignInSuccess(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                if (exception instanceof MsalUiRequiredException) {
                    AppCenterLog.info(LOG_TAG, "No token in cache, proceed with interactive sign-in experience.");
                    postOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            signInInteractively();
                        }
                    });
                } else {
                    handleSignInError(exception);
                }
            }

            @Override
            public void onCancel() {
                handleSignInCancellation();
            }
        });
    }

    private void handleSignInSuccess(final IAuthenticationResult authenticationResult) {
        post(new Runnable() {

            @Override
            public void run() {
                IAccount account = authenticationResult.getAccount();
                String homeAccountId = account.getHomeAccountIdentifier().getIdentifier();
                Date expiresOn = authenticationResult.getExpiresOn();
                AuthTokenContext authTokenContext = AuthTokenContext.getInstance();
                authTokenContext.setAuthToken(authenticationResult.getIdToken(), homeAccountId, expiresOn);
                String accountId = account.getAccountIdentifier().getIdentifier();
                AppCenterLog.info(LOG_TAG, "User sign-in succeeded.");
                completeSignIn(new UserInformation(accountId), null);
            }
        });
    }

    private void handleSignInError(MsalException exception) {
        AppCenterLog.error(LOG_TAG, "User sign-in failed.", exception);
        completeSignIn(null, exception);
    }

    private void handleSignInCancellation() {
        AppCenterLog.warn(LOG_TAG, "User canceled sign-in.");
        completeSignIn(null, new CancellationException("User cancelled sign-in."));
    }

    private synchronized void completeSignIn(UserInformation userInformation, Exception exception) {
        if (mPendingSignInFuture != null) {
            mPendingSignInFuture.complete(new SignInResult(userInformation, exception));
            mPendingSignInFuture = null;
        }
    }
}
