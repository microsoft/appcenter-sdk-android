package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.AbstractTokenContextListener;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.SERVICE_NAME;
import static com.microsoft.appcenter.storage.Constants.STORAGE_GROUP;

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
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Current API base URL.
     */
    private String mApiUrl = DEFAULT_API_URL;

    private Map<DefaultAppCenterFuture<?>, ServiceCall> mPendingCalls = new HashMap<>();

    private HttpClient mHttpClient;

    /**
     * Authorization listener for {@link AuthTokenContext}.
     */
    private AuthTokenContext.Listener mAuthListener;

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
    @SuppressWarnings({"SameParameterValue", "WeakerAccess", "unused"})
    // TODO Remove suppress warnings after reflection removed in test app
    public static void setApiUrl(String apiUrl) {
        getInstance().setInstanceApiUrl(apiUrl);
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

    /**
     * Read a document.
     * The document type (T) must be JSON deserializable.
     */
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId, Class<T> documentType) {
        AppCenterLog.debug(LOG_TAG, "Read started");
        return getInstance().instanceRead(partition, documentId, documentType);
    }

    /**
     * List (need optional signature to configure page size).
     * The document type (T) must be JSON deserializable.
     */
    public static <T> AppCenterFuture<PaginatedDocuments<T>> list(String partition, Class<T> documentType) {
        return getInstance().instanceList(partition, documentType);
    }

    /**
     * Create a document.
     * The document instance (T) must be JSON serializable.
     */
    public static <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document, Class<T> documentType) {
        AppCenterLog.debug(LOG_TAG, "Create started");
        return getInstance().instanceCreate(partition, documentId, document, documentType);
    }

    /**
     * Delete a document.
     */
    public static AppCenterFuture<Document<Void>> delete(String partition, String documentId) {
        return getInstance().instanceDelete(partition, documentId);
    }

    /**
     * Replace a document.
     * The document instance (T) must be JSON serializable.
     */
    public static <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document) {
        return null;
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    private synchronized void setInstanceApiUrl(String apiUrl) {
        mApiUrl = apiUrl;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mHttpClient = createHttpClient(context);
        mAppSecret = appSecret;
        mAuthListener = new AbstractTokenContextListener() {
            
            @Override
            public void onNewUser(String authToken) {
                if (authToken == null) {
                    TokenManager.getInstance().removeAllCachedTokens();
                }
            }
        };
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
            AuthTokenContext.getInstance().addListener(mAuthListener);
        } else {
            for (Map.Entry<DefaultAppCenterFuture<?>, ServiceCall> call : mPendingCalls.entrySet()) {
                call.getKey().complete(null);
                call.getValue().cancel();
            }
            AuthTokenContext.getInstance().removeListener(mAuthListener);
            mPendingCalls.clear();
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

    private synchronized <T> AppCenterFuture<Document<T>> instanceRead(
            final String partition,
            final String documentId,
            final Class<T> documentType) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();
        getTokenAndCallCosmosDbApi(
                partition,
                result,
                new TokenExchange.TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(TokenResult tokenResult) {
                        callCosmosDbReadApi(tokenResult, documentId, documentType, result);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
        return result;
    }

    private synchronized <T> void callCosmosDbReadApi(
            final TokenResult tokenResult,
            final String documentId,
            final Class<T> documentType,
            final DefaultAppCenterFuture<Document<T>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                documentId,
                mHttpClient,
                METHOD_GET,
                null,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        completeFutureAndRemovePendingCall(Utils.parseDocument(payload, documentType), result);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    /**
     * Create a document
     * The document type (T) must be JSON deserializable
     */
    private synchronized <T> AppCenterFuture<PaginatedDocuments<T>> instanceList(final String partition, final Class<T> documentType) {
        final DefaultAppCenterFuture<PaginatedDocuments<T>> result = new DefaultAppCenterFuture<>();
        getTokenAndCallCosmosDbApi(
                partition,
                result,
                new TokenExchange.TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(TokenResult tokenResult) {
                        callCosmosDbListApi(tokenResult, result, documentType);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        completeFutureAndRemovePendingCallWhenDocuments(e, result);
                    }
                });
        return result;
    }

    private synchronized <T> void callCosmosDbListApi(
            final TokenResult tokenResult,
            final DefaultAppCenterFuture<PaginatedDocuments<T>> result,
            final Class<T> documentType) {
        CosmosDb.callCosmosDbListApi(
                tokenResult,
                null,
                mHttpClient,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        Page<T> page = Utils.parseDocuments(payload, documentType);
                        PaginatedDocuments<T> paginatedDocuments = new PaginatedDocuments<T>()
                                .withCurrentPage(page).withTokenResult(tokenResult)
                                .withHttpClient(mHttpClient)
                                .withContinuationToken(headers.get(Constants.CONTINUATION_TOKEN_HEADER))
                                .withDocumentType(documentType);
                        completeFutureAndRemovePendingCallWhenDocuments(paginatedDocuments, result);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFutureAndRemovePendingCallWhenDocuments(e, result);
                    }
                });
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized <T> AppCenterFuture<Document<T>> instanceCreate(final String partition, final String documentId, final T document, final Class<T> documentType) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();
        getTokenAndCallCosmosDbApi(
                partition,
                result,
                new TokenExchange.TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        callCosmosDbCreateApi(tokenResult, document, documentType, partition, documentId, result);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
        return result;
    }

    private synchronized <T> void callCosmosDbCreateApi(
            final TokenResult tokenResult,
            T document,
            final Class<T> documentType,
            String partition,
            final String documentId,
            final DefaultAppCenterFuture<Document<T>> result) {
        CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                new Document<>(document, partition, documentId).toString(),
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        completeFutureAndRemovePendingCall(Utils.parseDocument(payload, documentType), result);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
    }

    private synchronized AppCenterFuture<Document<Void>> instanceDelete(final String partition, final String documentId) {
        final DefaultAppCenterFuture<Document<Void>> result = new DefaultAppCenterFuture<>();
        getTokenAndCallCosmosDbApi(
                partition,
                result,
                new TokenExchange.TokenExchangeServiceCallback() {
                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        callCosmosDbDeleteApi(tokenResult, documentId, result);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
        return result;
    }

    private synchronized void callCosmosDbDeleteApi(TokenResult tokenResult, String documentId, final DefaultAppCenterFuture<Document<Void>> result) {
        CosmosDb.callCosmosDbApi(
                tokenResult,
                documentId,
                mHttpClient,
                METHOD_DELETE,
                null,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        completeFutureAndRemovePendingCall(new Document<Void>(), result);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFutureAndRemovePendingCall(e, result);
                    }
                });
    }

    private synchronized <T> void getTokenAndCallCosmosDbApi(String partition, DefaultAppCenterFuture result, TokenExchange.TokenExchangeServiceCallback callback) {
        TokenResult tokenResult = TokenManager.getInstance().getCachedToken(partition);
        if (tokenResult != null) {
            callback.callCosmosDb(tokenResult);
        } else {
            ServiceCall tokenExchangeServiceCall =
                    TokenExchange.getDbToken(
                            partition,
                            mHttpClient,
                            mApiUrl,
                            mAppSecret,
                            callback);
            mPendingCalls.put(result, tokenExchangeServiceCall);
        }
    }

    private synchronized <T> void completeFutureAndRemovePendingCall(Document<T> value, DefaultAppCenterFuture<Document<T>> result) {
        result.complete(value);
        mPendingCalls.remove(result);
    }

    private synchronized <T> void completeFutureAndRemovePendingCall(Exception e, DefaultAppCenterFuture<Document<T>> future) {
        Utils.handleApiCallFailure(e);
        future.complete(new Document<T>(e));
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFutureAndRemovePendingCallWhenDocuments(PaginatedDocuments<T> value, DefaultAppCenterFuture<PaginatedDocuments<T>> result) {
        result.complete(value);
        mPendingCalls.remove(result);
    }

    private synchronized <T> void completeFutureAndRemovePendingCallWhenDocuments(Exception e, DefaultAppCenterFuture<PaginatedDocuments<T>> future) {
        Utils.handleApiCallFailure(e);
        future.complete(new PaginatedDocuments<T>());
        mPendingCalls.remove(future);
    }
}
