package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.ConflictResolutionPolicy;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Documents;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.net.URL;
import java.util.Map;

import static com.microsoft.appcenter.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.*;

/**
 * Storage service.
 */
public class Storage extends AbstractAppCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Storage sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Current API base URL.
     */
    private String mApiUrl = DEFAULT_API_URL;

    private final Gson gson;

    private Storage() {
        gson = new Gson();
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Storage getInstance() {
        if (sInstance == null) {
            sInstance = new Storage();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Change the base URL used to make API calls.
     *
     * @param apiUrl API base URL.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static void setApiUrl(String apiUrl) {
        getInstance().setInstanceApiUrl(apiUrl);
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    private synchronized void setInstanceApiUrl(String apiUrl) {
        mApiUrl = apiUrl;
    }

    /**
     * Check whether Storage service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Storage service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
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
        } else {
        }
    }

    @Override
    protected String getGroupName() {
        return STORAGE_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }



    // Read a document
    // The document type (T) must be JSON deserializable
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId){
        AppCenterLog.debug(LOG_TAG, "Read started");
        return getInstance().instanceRead(partition, documentId);
    }

    // Read a document
    // The document type (T) must be JSON deserializable
    private synchronized  <T> AppCenterFuture<Document<T>> instanceRead(final String partition, final String documentId){
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();

        TokenExchange.getDbToken( partition , mContext, mApiUrl, mAppSecret, new ServiceCallback() {
            @Override
            public void onCallSucceeded(final String payload, Map<String, String> headers) {
                TokensResponse tokensResponse = gson.fromJson(payload, TokensResponse.class);
                final TokenResult tokenResult = tokensResponse.tokens().get(0);

                //TODO Check if the token exchange status has succeded
                // https://docs.microsoft.com/en-us/rest/api/cosmos-db/get-a-document
                CosmosDb.callCosmosDb(tokenResult.dbAccount(),
                        tokenResult.dbName(),
                        tokenResult.dbCollectionName(),
                        documentId,
                        tokenResult.partition(),
                        tokenResult.token(),
                        mContext,
                        METHOD_GET ,
                        new HttpClient.CallTemplate() {

                            @Override
                            public String buildRequestBody() { return ""; }

                            @Override
                            public void onBeforeCalling(URL url, Map<String, String> headers) { }
                        }, new ServiceCallback() {

                            @Override
                            public void onCallSucceeded(final String payload, Map<String, String> headers) {
                                result.complete(new Document<T>(payload, tokenResult.partition(), documentId));
                            }

                            @Override
                            public void onCallFailed(Exception e) {
                                handleApiCallFailure(e);

                                // TODO: construct an error object
                                result.complete(null);
                            }
                        });

                // TODO: do we need to call `complete` here?
            }

            @Override
            public void onCallFailed(Exception e) {
                handleApiCallFailure(e);

                // TODO: construct an error object
                result.complete(null);
            }
        });

        return result;
    }

    // List (need optional signature to configure page size)
    // The document type (T) must be JSON deserializable
    public <T> AppCenterFuture<Documents<T>> list(String partition, Class<T> documentType) {
        return null;
    }

    /**
     * Create a document
     * The document instance (T) must be JSON serializable
     * @param partition
     * @param documentId
     * @param document
     * @param <T>
     * @return
     */

    public static <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document) {
        // https://docs.microsoft.com/en-us/rest/api/cosmos-db/create-a-document

        AppCenterLog.debug(LOG_TAG, "Create started");
        getInstance().instanceCreate(partition, documentId, document);
        return null;
    }

    // Read a document
    // The document type (T) must be JSON deserializable
    private synchronized  <T> AppCenterFuture<Document<T>> instanceCreate(final String partition, final String documentId, final T document){
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();

        TokenExchange.getDbToken( partition , mContext, mApiUrl, mAppSecret, new ServiceCallback() {
            @Override
            public void onCallSucceeded(final String payload, Map<String, String> headers) {
                TokensResponse tokensResponse = gson.fromJson(payload, TokensResponse.class);
                final TokenResult tokenResult = tokensResponse.tokens().get(0);

                // https://docs.microsoft.com/en-us/rest/api/cosmos-db/get-a-document
                CosmosDb.callCosmosDb(tokenResult.dbAccount(),
                        tokenResult.dbName(),
                        tokenResult.dbCollectionName(),
                        null,
                        tokenResult.partition(),
                        tokenResult.token(),
                        mContext,
                        METHOD_POST ,
                        new HttpClient.CallTemplate() {

                            @Override
                            public String buildRequestBody() {
                                return ((new Document<T>(document, partition, documentId)).toString());
                            }

                            @Override
                            public void onBeforeCalling(URL url, Map<String, String> headers) { }
                        }, new ServiceCallback() {

                            @Override
                            public void onCallSucceeded(final String payload, Map<String, String> headers) {
                                result.complete(new Document<T>(payload, tokenResult.partition(), documentId));
                            }

                            @Override
                            public void onCallFailed(Exception e) {
                                handleApiCallFailure(e);

                                // TODO: construct an error object
                                result.complete(null);
                            }
                        });

                // TODO: do we need to call `complete` here?
            }

            @Override
            public void onCallFailed(Exception e) {
                handleApiCallFailure(e);

                // TODO: construct an error object
                result.complete(null);
            }
        });

        return result;
    }

    public <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document, ConflictResolutionPolicy resolutionPolicy) {
        //docDbClient.Create(documentId)
        return null;
    }

    // Replace a document
    // The document instance (T) must be JSON serializable
    public <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document){
        return null;
    }

    public <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document, ConflictResolutionPolicy resolutionPolicy) {
        return null;
    }

    // Delete a document
    public AppCenterFuture<Document<Void>> delete(String partition, String documentId) {
        return null;
    }

    /**
     * Handle API call failure.
     */
    private synchronized void handleApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(LOG_TAG, "Exception", httpException);
            }
        }
    }
}
