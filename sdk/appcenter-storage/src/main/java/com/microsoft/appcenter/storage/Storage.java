/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.StorageHttpClientDecorator;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.client.TokenExchange.TokenExchangeServiceCallback;
import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.DataStoreEventListener;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.DocumentError;
import com.microsoft.appcenter.storage.models.DocumentMetadata;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.storage.models.PendingOperation;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.AbstractTokenContextListener;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.storage.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.storage.Constants.SERVICE_NAME;
import static com.microsoft.appcenter.storage.Constants.STORAGE_GROUP;

/**
 * Storage service.
 */
public class Storage extends AbstractAppCenterService implements NetworkStateHelper.Listener {

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

    private StorageHttpClientDecorator mHttpClient;

    private LocalDocumentStorage mLocalDocumentStorage;

    /**
     * Current event listener.
     */
    private static DataStoreEventListener mEventListener;

    /**
     * Authorization listener for {@link AuthTokenContext}.
     */
    private AuthTokenContext.Listener mAuthListener;

    private NetworkStateHelper mNetworkStateHelper;

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
     * Check whether offline mode is enabled or not.
     *
     * @return result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static boolean isOfflineMode() {
        return getInstance().isOfflineModeInstance();
    }

    /**
     * Enable or disable offline mode.
     *
     * @param offlineMode <code>true</code> to simulate device being offline, <code>false</code> to go back to the original network state of the device.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static void setOfflineMode(boolean offlineMode) {
        getInstance().setOfflineModeInstance(offlineMode);
    }

    /**
     * Check whether offline mode is enabled or not.
     *
     * @return result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    private synchronized boolean isOfflineModeInstance() {
        if (mHttpClient != null) {
            return mHttpClient.isOfflineMode();
        }
        AppCenterLog.error(LOG_TAG, "AppCenter Storage must be started before checking if offline mode is enabled.");
        return false;
    }

    /**
     * Enable or disable offline mode.
     *
     * @param offlineMode <code>true</code> to simulate device being offline, <code>false</code> to go back to the original network state of the device.
     */
    private synchronized void setOfflineModeInstance(boolean offlineMode) {
        if (mHttpClient != null) {
            mHttpClient.setOfflineMode(offlineMode);
        } else {
            AppCenterLog.error(LOG_TAG, "AppCenter Storage must be started before setting offline mode.");
        }
    }

    /**
     * Read a document.
     * The document type (T) must be JSON deserializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId, Class<T> documentType) {
        return read(partition, documentId, documentType, new ReadOptions());
    }

    /**
     * Read a document.
     * The document type (T) must be JSON deserializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> read(String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
        return getInstance().instanceRead(partition, documentId, documentType, readOptions);
    }

    /**
     * List (need optional signature to configure page size).
     * The document type (T) must be JSON deserializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<PaginatedDocuments<T>> list(String partition, Class<T> documentType) {
        return getInstance().instanceList(partition, documentType);
    }

    /**
     * Create a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document, Class<T> documentType) {
        return create(partition, documentId, document, documentType, new WriteOptions());
    }

    /**
     * Create a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> create(String partition, String documentId, T document, Class<T> documentType, WriteOptions writeOptions) {
        return getInstance().instanceCreateOrUpdate(partition, documentId, document, documentType, writeOptions);
    }

    /**
     * Delete a document.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static AppCenterFuture<Document<Void>> delete(String partition, String documentId) {
        return getInstance().instanceDelete(partition, documentId);
    }

    /**
     * Replace a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document, Class<T> documentType) {
        return replace(partition, documentId, document, documentType, new WriteOptions());
    }

    /**
     * Replace a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<Document<T>> replace(String partition, String documentId, T document, Class<T> documentType, WriteOptions writeOptions) {

        /* In the current version we do not support E-tag optimistic concurrency logic and `replace` will call Create (POST) operation instead of Replace (PUT). */
        AppCenterLog.debug(LOG_TAG, "Replace started");
        return Storage.create(partition, documentId, document, documentType, writeOptions);
    }

    /**
     * Sets a listener that will be invoked on network status change to notify of pending operations execution status.
     * Pass null to unregister.
     *
     * @param listener to notify on remote operations or null to unregister the previous listener.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static void setDataStoreRemoteOperationListener(DataStoreEventListener listener) {
        mEventListener = listener;
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    private synchronized void setInstanceApiUrl(String apiUrl) {
        mApiUrl = apiUrl;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mNetworkStateHelper = NetworkStateHelper.getSharedInstance(context);
        mHttpClient = new StorageHttpClientDecorator(createHttpClient(context));
        mAppSecret = appSecret;
        mLocalDocumentStorage = new LocalDocumentStorage(context);
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
     * Called whenever the network state is updated.
     *
     * @param connected true if connected, false otherwise.
     */
    @Override
    public void onNetworkStateUpdated(boolean connected) {

        /* If device comes back online. */
        if (connected) {
            for (PendingOperation po : mLocalDocumentStorage.getPendingOperations()) {
                if (PENDING_OPERATION_CREATE_VALUE.equals(po.getOperation()) ||
                        PENDING_OPERATION_REPLACE_VALUE.equals(po.getOperation())) {
                    instanceCreateOrUpdate(po);
                } else if (PENDING_OPERATION_DELETE_VALUE.equals(po.getOperation())) {
                    instanceDelete(po);
                } else {
                    AppCenterLog.debug(LOG_TAG, String.format("Pending operation '%s' is not supported", po.getOperation()));
                }
            }
        }
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
            mNetworkStateHelper.addListener(this);
        } else {
            for (Map.Entry<DefaultAppCenterFuture<?>, ServiceCall> call : mPendingCalls.entrySet()) {
                call.getKey().complete(null);
                call.getValue().cancel();
            }
            AuthTokenContext.getInstance().removeListener(mAuthListener);
            mNetworkStateHelper.removeListener(this);
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
            final Class<T> documentType,
            final ReadOptions readOptions) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();
        if (mNetworkStateHelper.isNetworkConnected()) {
            getTokenAndCallCosmosDbApi(
                    partition,
                    result,
                    new TokenExchangeServiceCallback() {

                        @Override
                        public void callCosmosDb(TokenResult tokenResult) {
                            callCosmosDbReadApi(tokenResult, documentId, documentType, result);
                        }

                        @Override
                        public void completeFuture(Exception e) {
                            Storage.this.completeFuture(e, result);
                        }
                    });
        } else {
            Document<T> cachedDocument = mLocalDocumentStorage.read(partition, documentId, documentType, readOptions);
            result.complete(cachedDocument);
        }
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
                        completeFutureAndSaveToLocalStorage(Utils.parseDocument(payload, documentType), result, null);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
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
                new TokenExchangeServiceCallback() {

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
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbListApi(
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
                        completeFuture(paginatedDocuments, result);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFutureAndRemovePendingCallWhenDocuments(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized <T> AppCenterFuture<Document<T>> instanceCreateOrUpdate(
            final String partition,
            final String documentId,
            final T document,
            final Class<T> documentType,
            final WriteOptions writeOptions) {
        final DefaultAppCenterFuture<Document<T>> result = new DefaultAppCenterFuture<>();
        getTokenAndCallCosmosDbApi(
                partition,
                result,
                new TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        callCosmosDbCreateOrUpdateApi(tokenResult, document, documentType, partition, documentId, writeOptions, result);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        Storage.this.completeFuture(e, result);
                    }
                });
        return result;
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized void instanceCreateOrUpdate(
            final PendingOperation pendingOperation) {
        getTokenAndCallCosmosDbApi(
                pendingOperation.getPartition(),
                null,
                new TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        callCosmosDbCreateOrUpdateApi(tokenResult, pendingOperation);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new StorageException("Failed to get Cosmos DB token for performing a create or update operation.", e),
                                pendingOperation);
                    }
                });
    }

    private synchronized <T> void callCosmosDbCreateOrUpdateApi(
            final TokenResult tokenResult,
            T document,
            final Class<T> documentType,
            String partition,
            final String documentId,
            final WriteOptions writeOptions,
            final DefaultAppCenterFuture<Document<T>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                new Document<>(document, partition, documentId).toString(),
                CosmosDb.GetUpsertAdditionalHeader(),
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        Document<T> cosmosDbDocument = Utils.parseDocument(payload, documentType);
                        completeFuture(cosmosDbDocument, result);
                        mLocalDocumentStorage.write(cosmosDbDocument, writeOptions);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    private synchronized <T> void callCosmosDbCreateOrUpdateApi(
            final TokenResult tokenResult,
            final PendingOperation pendingOperation) {
        CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                pendingOperation.getDocument(),
                CosmosDb.GetUpsertAdditionalHeader(),
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        notifyListenerAndUpdateOperationOnSuccess(payload, pendingOperation);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new StorageException("Failed to call Cosmos create or replace API", e),
                                pendingOperation);
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
                        Storage.this.completeFuture(e, result);
                    }
                });
        return result;
    }

    private synchronized void instanceDelete(final PendingOperation pendingOperation) {
        getTokenAndCallCosmosDbApi(
                pendingOperation.getPartition(),
                null,
                new TokenExchange.TokenExchangeServiceCallback() {

                    @Override
                    public void callCosmosDb(TokenResult tokenResult) {
                        callCosmosDbDeleteApi(tokenResult, pendingOperation);
                    }

                    @Override
                    public void completeFuture(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new StorageException("Failed to get Cosmos DB token for performing a delete operation.", e),
                                pendingOperation);
                    }
                });
    }

    private synchronized void callCosmosDbDeleteApi(final TokenResult tokenResult, final String documentId, final DefaultAppCenterFuture<Document<Void>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                documentId,
                mHttpClient,
                METHOD_DELETE,
                null,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        completeFuture(new Document<Void>(), result);
                        mLocalDocumentStorage.delete(tokenResult.partition(), documentId);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    private synchronized void callCosmosDbDeleteApi(TokenResult tokenResult, final PendingOperation pendingOperation) {
        CosmosDb.callCosmosDbApi(
                tokenResult,
                pendingOperation.getDocumentId(),
                mHttpClient,
                METHOD_DELETE,
                null,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        notifyListenerAndUpdateOperationOnSuccess(payload, pendingOperation);
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new StorageException("Failed to call Cosmos delete API", e),
                                pendingOperation);
                    }
                });
    }

    synchronized void getTokenAndCallCosmosDbApi(
            String partition,
            DefaultAppCenterFuture result,
            TokenExchangeServiceCallback callback) {
        TokenResult cachedTokenResult = TokenManager.getInstance().getCachedToken(partition);
        if (cachedTokenResult != null) {
            callback.callCosmosDb(cachedTokenResult);
        } else {
            ServiceCall tokenExchangeServiceCall =
                    TokenExchange.getDbToken(
                            partition,
                            mHttpClient,
                            mApiUrl,
                            mAppSecret,
                            callback);
            if (result != null) {
                mPendingCalls.put(result, tokenExchangeServiceCall);
            }
        }
    }

    private synchronized <T> void completeFuture(T value, DefaultAppCenterFuture<T> future) {
        future.complete(value);
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFutureAndSaveToLocalStorage(T value, DefaultAppCenterFuture<T> future, String pendingOperationValue) {
        future.complete(value);
        mLocalDocumentStorage.write((Document)value, new WriteOptions(), pendingOperationValue);
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFuture(Exception e, DefaultAppCenterFuture<Document<T>> future) {
        Utils.logApiCallFailure(e);
        future.complete(new Document<T>(e));
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFutureAndRemovePendingCallWhenDocuments(Exception e, DefaultAppCenterFuture<PaginatedDocuments<T>> future) {
        Utils.logApiCallFailure(e);
        future.complete(new PaginatedDocuments<T>().withCurrentPage(new Page<T>(e)));
        mPendingCalls.remove(future);
    }

    private synchronized void notifyListenerAndUpdateOperationOnSuccess(String cosmosDbResponsePayload, PendingOperation pendingOperation) {
        String etag = Utils.getEtag(cosmosDbResponsePayload);
        pendingOperation.setEtag(etag);
        pendingOperation.setDocument(cosmosDbResponsePayload);
        if (mEventListener != null) {
            mEventListener.onDataStoreOperationResult(
                    pendingOperation.getOperation(),
                    new DocumentMetadata(
                            pendingOperation.getPartition(),
                            pendingOperation.getDocumentId(),
                            etag),
                    null);
        }
        mLocalDocumentStorage.updateLocalCopy(pendingOperation);
    }

    private synchronized void notifyListenerAndUpdateOperationOnFailure(StorageException e, PendingOperation pendingOperation) {
        AppCenterLog.error(LOG_TAG, "Remote operation failed", e);
        boolean deleteLocalCopy = false;
        if (e.getCause() instanceof HttpException) {
            switch (((HttpException) e.getCause()).getStatusCode()) {
                /* The document was removed on the server. */
                case 404:
                /* Partition and document_id combination is already present in the DB. */
                case 409:
                    deleteLocalCopy = true;
                    break;
            }
        }
        if (mEventListener != null) {
            mEventListener.onDataStoreOperationResult(
                    pendingOperation.getOperation(),
                    null,
                    new DocumentError(e));
        }
        if (deleteLocalCopy) {
            mLocalDocumentStorage.delete(pendingOperation);
        } else {
            mLocalDocumentStorage.updateLocalCopy(pendingOperation);
        }
    }
}
