package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Documents;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.util.Map;

import static com.microsoft.appcenter.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
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
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, "Read started");
        return getInstance().instanceRead(partition, documentId);
    }

    // Read a document
    // The document type (T) must be JSON deserializable
    private synchronized <T> AppCenterFuture<Document<T>> instanceRead(final String partition, final String documentId) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();

        TokenExchange.getDbToken(
                partition,
                createHttpClient(mContext),
                mApiUrl,
                mAppSecret,
                new TokenExchange.TokenExchangeServiceCallback() {
                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {

                        /* https://docs.microsoft.com/en-us/rest/api/cosmos-db/get-a-document */
                        CosmosDb.callCosmosDb(
                                tokenResult,
                                documentId,
                                createHttpClient(mContext),
                                METHOD_GET,
                                "",
                                new ServiceCallback() {

                                    @Override
                                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                                        result.complete(new Document<T>(payload, tokenResult.partition(), documentId));
                                    }

                                    @Override
                                    public void onCallFailed(Exception e) {
                                        handleApiCallFailure(e);
                                        result.complete(new Document<T>(e));
                                    }
                                });

                        // TODO: do we need to call `complete` here?
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        result.complete(new Document<T>(e));
                    }
                });

        return result;
    }

    private TokenResult parseTokenResult(String payload) {
        TokensResponse tokensResponse = gson.fromJson(payload, TokensResponse.class);
        return tokensResponse.tokens().get(0);
    }

    // List (need optional signature to configure page size)
    // The document type (T) must be JSON deserializable
    public <T> AppCenterFuture<Documents<T>> list(String partition, Class<T> documentType) {
        return null;
    }

    /**
     * Create a document
     * The document instance (T) must be JSON serializable
     *
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

    // Create a document
    // The document type (T) must be JSON deserializable
    private synchronized <T> AppCenterFuture<Document<T>> instanceCreate(final String partition, final String documentId, final T document) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();

        TokenExchange.getDbToken(
                partition,
                createHttpClient(mContext),
                mApiUrl,
                mAppSecret,
                new TokenExchange.TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        CosmosDb.callCosmosDb(
                                tokenResult,
                                null,
                                createHttpClient(mContext),
                                METHOD_POST,
                                new Document<T>(document, partition, documentId).toString(),
                                new ServiceCallback() {

                                    @Override
                                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                                        result.complete(new Document<T>(payload, tokenResult.partition(), documentId));
                                    }

                                    @Override
                                    public void onCallFailed(Exception e) {
                                        handleApiCallFailure(e);
                                        result.complete(new Document<T>(e));

                                    }
                                });

                        // TODO: do we need to call `complete` here?
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        result.complete(new Document<T>(e));
                    }
                });

        return result;
    }

    // Replace a document
    // The document instance (T) must be JSON serializable
    public <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document) {
        return null;
    }

    // Delete a document
    public static AppCenterFuture<Document<Void>> delete(String partition, String documentId) {

        AppCenterLog.debug(LOG_TAG, "Delete started");
        getInstance().instanceDelete(partition, documentId);
        return null;
    }

    // Delete a document
    // The document type (T) must be JSON deserializable
    private synchronized AppCenterFuture<Document<Void>> instanceDelete(final String partition, final String documentId) {
        final DefaultAppCenterFuture<Document<Void>> result = new DefaultAppCenterFuture<>();

        TokenExchange.getDbToken(
                partition,
                createHttpClient(mContext),
                mApiUrl,
                mAppSecret,
                new TokenExchange.TokenExchangeServiceCallback() {
                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        // https://docs.microsoft.com/en-us/rest/api/cosmos-db/get-a-document
                        CosmosDb.callCosmosDb(
                                tokenResult,
                                documentId,
                                createHttpClient(mContext),
                                METHOD_DELETE,
                                "",
                                new ServiceCallback() {

                                    @Override
                                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                                        result.complete(new Document<Void>((null)));
                                    }

                                    @Override
                                    public void onCallFailed(Exception e) {
                                        handleApiCallFailure(e);
                                        result.complete(new Document<Void>(e));
                                    }
                                });

                        // TODO: do we need to call `complete` here?
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        result.complete(new Document<Void>(e));
                    }
                });

        return result;
    }

}
