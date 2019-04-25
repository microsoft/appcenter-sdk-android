/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.UserInformation;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.client.TokenExchange.TokenExchangeServiceCallback;
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DataStoreEventListener;
import com.microsoft.appcenter.data.models.DocumentMetadata;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
import com.microsoft.appcenter.data.models.PendingOperation;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.TokenResult;
import com.microsoft.appcenter.data.models.WriteOptions;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.AbstractTokenContextListener;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.data.Constants.DATA_GROUP;
import static com.microsoft.appcenter.data.Constants.DEFAULT_API_URL;
import static com.microsoft.appcenter.data.Constants.LOG_TAG;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.data.Constants.SERVICE_NAME;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;

/**
 * Data service.
 */
public class Data extends AbstractAppCenterService implements NetworkStateHelper.Listener {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Data sInstance;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Current token exchange base URL.
     */
    private String mTokenExchangeUrl = DEFAULT_API_URL;

    private Map<DefaultAppCenterFuture<?>, ServiceCall> mPendingCalls = new HashMap<>();

    private final HashMap<String, ServiceCall> mOutgoingPendingOperationCalls = new HashMap<>();

    private HttpClient mHttpClient;

    private TokenManager mTokenManager;

    private LocalDocumentStorage mLocalDocumentStorage;

    /**
     * Current event listener.
     */
    private volatile DataStoreEventListener mEventListener;

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
    public static synchronized Data getInstance() {
        if (sInstance == null) {
            sInstance = new Data();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Change the URL used to retrieve CosmosDB resource tokens.
     *
     * @param tokenExchangeUrl Token Exchange service URL.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess", "unused"})
    // TODO Remove suppress warnings after reflection removed in test app
    public static void setTokenExchangeUrl(String tokenExchangeUrl) {
        getInstance().setInstanceTokenExchangeUrl(tokenExchangeUrl);
    }

    /**
     * Check whether Data service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Data service.
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
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> read(String documentId, Class<T> documentType, String partition) {
        return read(documentId, documentType, partition, new ReadOptions());
    }

    /**
     * Read a document.
     * The document type (T) must be JSON deserializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> read(String documentId, Class<T> documentType, String partition, ReadOptions readOptions) {
        return getInstance().instanceRead(documentId, documentType, partition, readOptions);
    }

    /**
     * List (need optional signature to configure page size).
     * The document type (T) must be JSON deserializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<PaginatedDocuments<T>> list(Class<T> documentType, String partition) {
        return getInstance().instanceList(documentType, partition);
    }

    /**
     * Create a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> create(String documentId, T document, Class<T> documentType, String partition) {
        return create(documentId, document, documentType, partition, new WriteOptions());
    }

    /**
     * Create a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> create(String documentId, T document, Class<T> documentType, String partition, WriteOptions writeOptions) {
        return getInstance().instanceCreateOrUpdate(documentId, document, documentType, partition, writeOptions, null);
    }

    /**
     * Delete a document.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static AppCenterFuture<DocumentWrapper<Void>> delete(String documentId, String partition) {
        return delete(documentId, partition, new WriteOptions());
    }

    /**
     * Delete a document.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static AppCenterFuture<DocumentWrapper<Void>> delete(String documentId, String partition, WriteOptions writeOptions) {
        return getInstance().instanceDelete(documentId, partition, writeOptions);
    }

    /**
     * Replace a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> replace(String documentId, T document, Class<T> documentType, String partition) {
        return replace(documentId, document, documentType, partition, new WriteOptions());
    }

    /**
     * Replace a document.
     * The document instance (T) must be JSON serializable.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static <T> AppCenterFuture<DocumentWrapper<T>> replace(String documentId, T document, Class<T> documentType, String partition, WriteOptions writeOptions) {
        return getInstance().instanceCreateOrUpdate(documentId, document, documentType, partition, writeOptions, CosmosDb.getUpsertAdditionalHeader());
    }

    /**
     * Sets a listener that will be invoked on network status change to notify of pending operations execution status.
     * Pass null to unregister.
     *
     * @param listener to notify on remote operations or null to unregister the previous listener.
     */
    @SuppressWarnings("WeakerAccess") // TODO remove warning suppress after release.
    public static void setDataStoreRemoteOperationListener(DataStoreEventListener listener) {
        getInstance().mEventListener = listener;
    }

    /**
     * Implements {@link #setTokenExchangeUrl(String)}}.
     */
    private synchronized void setInstanceTokenExchangeUrl(String tokenExchangeUrl) {
        mTokenExchangeUrl = tokenExchangeUrl;
    }

    private static DataException getInvalidPartitionDataException(String partition) {
        return new DataException(String.format("Partition name can be either '%s' or '%s' but not '%s'.", APP_DOCUMENTS, USER_DOCUMENTS, partition));
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mNetworkStateHelper = NetworkStateHelper.getSharedInstance(context);
        mHttpClient = createHttpClient(context);
        mTokenManager = TokenManager.getInstance(context);
        mAppSecret = appSecret;
        mLocalDocumentStorage = new LocalDocumentStorage(context, Utils.getUserTableName());
        mAuthListener = new AbstractTokenContextListener() {

            @Override
            public void onNewUser(UserInformation userInfo) {
                if (userInfo == null) {
                    mTokenManager.removeAllCachedTokens();
                    mLocalDocumentStorage.resetDatabase();
                } else {
                    String userTable = Utils.getUserTableName(userInfo.getAccountId());
                    mLocalDocumentStorage.createTableIfDoesNotExist(userTable);
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
    public void onNetworkStateUpdated(final boolean connected) {

        /* If device comes back online. */
        if (connected) {
            post(new Runnable() {

                @Override
                public void run() {
                    processPendingOperations();
                }
            });
        }
    }

    private synchronized void processPendingOperations() {
        for (PendingOperation po : mLocalDocumentStorage.getPendingOperations(Utils.getUserTableName())) {
            String outgoingId = Utils.getOutgoingId(po.getPartition(), po.getDocumentId());

            /* If the operation is already being processed, skip it. */
            if (mOutgoingPendingOperationCalls.containsKey(outgoingId)) {
                continue;
            }

            /* Put the pending document id into the map to prevent further duplicate http call. The ServiceCall will be set when the http operation executes. */
            mOutgoingPendingOperationCalls.put(outgoingId, null);
            if (PENDING_OPERATION_CREATE_VALUE.equals(po.getOperation()) ||
                    PENDING_OPERATION_REPLACE_VALUE.equals(po.getOperation())) {
                instanceCreateOrUpdate(po);
            } else if (PENDING_OPERATION_DELETE_VALUE.equals(po.getOperation())) {
                instanceDelete(po);
            } else {
                AppCenterLog.debug(LOG_TAG, String.format("Pending operation '%s' is not supported.", po.getOperation()));
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
            if (mNetworkStateHelper.isNetworkConnected()) {
                processPendingOperations();
            }
        } else {
            for (Map.Entry<DefaultAppCenterFuture<?>, ServiceCall> call : mPendingCalls.entrySet()) {
                call.getKey().complete(null);
                call.getValue().cancel();
            }
            AuthTokenContext.getInstance().removeListener(mAuthListener);
            mNetworkStateHelper.removeListener(this);
            mPendingCalls.clear();
            for (Map.Entry<String, ServiceCall> call : mOutgoingPendingOperationCalls.entrySet()) {
                if (call.getValue() != null) {
                    call.getValue().cancel();
                }
            }
            mOutgoingPendingOperationCalls.clear();
        }
    }

    @Override
    protected String getGroupName() {
        return DATA_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }


    private <T> DefaultAppCenterFuture<DocumentWrapper<T>> performOperation(@NonNull final String partition,
                                                                            @NonNull final String documentId,
                                                                            @NonNull final Class<T> documentType,
                                                                            @Nullable final ReadOptions cacheReadOptions,
                                                                            @NonNull final CallTemplate<T> callTemplate) {

        /* Check partition is supported. */
        final DefaultAppCenterFuture<DocumentWrapper<T>> result = new DefaultAppCenterFuture<>();
        if (isInvalidPartition(partition, result)) {
            return result;
        }
        postAsyncGetter(new Runnable() {
            @Override
            public void run() {

                /* Get cached document. */
                DocumentWrapper<T> cachedDocument;
                String table = null;
                TokenResult cachedToken = getCachedToken(partition);
                if (cachedToken != null) {
                    table = Utils.getTableName(cachedToken);
                    cachedDocument = mLocalDocumentStorage.read(table, cachedToken.getPartition(), documentId, documentType, cacheReadOptions);
                    if (Constants.PENDING_OPERATION_DELETE_VALUE.equals(cachedDocument.getPendingOperation())) {
                        cachedDocument = new DocumentWrapper<>(new DataException("The document is found in local storage but marked as state deleted."));
                    }
                } else {
                    cachedDocument = new DocumentWrapper<>(new DataException("Unable to find partition named " + partition + "."));
                }

                /* Call template to see if online operation is needed. */
                if (callTemplate.needsRemoteOperation(cachedDocument)) {
                    if (mNetworkStateHelper.isNetworkConnected()) {
                        getTokenAndCallCosmosDbApi(
                                partition,
                                result,
                                new TokenExchangeServiceCallback(mTokenManager) {

                                    @Override
                                    public void callCosmosDb(TokenResult tokenResult) {
                                        callTemplate.callCosmosDb(tokenResult, result);
                                    }

                                    @Override
                                    public void completeFuture(Exception e) {
                                        Data.this.completeFuture(e, result);
                                    }
                                });
                    } else {
                        doOfflineOperation(cachedDocument, table, cachedToken, result, callTemplate);
                    }
                } else {
                    doOfflineOperation(cachedDocument, table, cachedToken, result, callTemplate);
                }
            }
        }, result, null); // TODO use a document error instead of null.
        return result;
    }

    private <T> void doOfflineOperation(DocumentWrapper<T> cachedDocument, String table, TokenResult cachedToken, DefaultAppCenterFuture<DocumentWrapper<T>> result, CallTemplate<T> callTemplate) {
        if (cachedToken == null) {

            /* If no token and offline, return the no partition error document previously initialized with that specific error. */
            result.complete(cachedDocument);
        } else {
            DocumentWrapper<T> documentResult = callTemplate.doOfflineOperation(cachedDocument, table, cachedToken);
            completeFuture(documentResult, result);
        }
    }

    @WorkerThread
    private synchronized <T> AppCenterFuture<DocumentWrapper<T>> instanceRead(
            final String documentId, final Class<T> documentType, final String partition,
            final ReadOptions readOptions) {
        return performOperation(partition, documentId, documentType, readOptions, new CallTemplate<T>() {

            @Override
            public boolean needsRemoteOperation(DocumentWrapper<T> cachedDocument) {
                return cachedDocument.getPendingOperation() == null;
            }

            @Override
            public DocumentWrapper<T> doOfflineOperation(DocumentWrapper<T> cachedDocument, String table, TokenResult cachedToken) {
                return cachedDocument;
            }

            @Override
            public void callCosmosDb(TokenResult tokenResult, DefaultAppCenterFuture<DocumentWrapper<T>> result) {
                callCosmosDbReadApi(tokenResult, documentId, documentType, result);
            }
        });
    }


    private synchronized AppCenterFuture<DocumentWrapper<Void>> instanceDelete(final String documentId, final String partition, final WriteOptions writeOptions) {
        return performOperation(partition, documentId, Void.class, null, new CallTemplate<Void>() {

            @Override
            public boolean needsRemoteOperation(DocumentWrapper<Void> cachedDocument) {
                return cachedDocument.getETag() != null || cachedDocument.getError() != null;
            }

            @Override
            public DocumentWrapper<Void> doOfflineOperation(DocumentWrapper<Void> cachedDocument, String table, TokenResult cachedToken) {
                boolean success;
                if (cachedDocument.getETag() != null) {
                    success =
                            mLocalDocumentStorage.deleteOffline(table, cachedToken.getPartition(), documentId, writeOptions);
                } else {
                    success = mLocalDocumentStorage.deleteOnline(table, cachedToken.getPartition(), documentId);
                }
                if (success) {
                    return new DocumentWrapper<>();
                } else {
                    return new DocumentWrapper<>(new DataException("Failed to write to cache."));
                }
            }

            @Override
            public void callCosmosDb(TokenResult tokenResult, DefaultAppCenterFuture<DocumentWrapper<Void>> result) {
                callCosmosDbDeleteApi(tokenResult, documentId, result);
            }
        });
    }

    private synchronized <T> void callCosmosDbReadApi(
            final TokenResult tokenResult,
            final String documentId,
            final Class<T> documentType,
            final DefaultAppCenterFuture<DocumentWrapper<T>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                documentId,
                mHttpClient,
                METHOD_GET,
                null,
                new ServiceCallback() {

                    @MainThread
                    @Override
                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                DocumentWrapper<T> document = Utils.parseDocument(payload, documentType);
                                if (document.getError() != null) {
                                    completeFutureOnDocumentError(document, result);
                                } else {
                                    completeFutureAndSaveToLocalStorage(Utils.getTableName(tokenResult), document, result);
                                }
                            }
                        });
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
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
                                .setCurrentPage(page).setTokenResult(tokenResult)
                                .setHttpClient(mHttpClient)
                                .setContinuationToken(headers.get(Constants.CONTINUATION_TOKEN_HEADER))
                                .setDocumentType(documentType);
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
     * Create a document
     * The document type (T) must be JSON deserializable
     */
    private synchronized <T> AppCenterFuture<PaginatedDocuments<T>> instanceList(final Class<T> documentType, final String partition) {
        final DefaultAppCenterFuture<PaginatedDocuments<T>> result = new DefaultAppCenterFuture<>();
        if (isInvalidPartitionWhenDocuments(partition, result)) {
            return result;
        }
        if (!mNetworkStateHelper.isNetworkConnected()) {
            completeFutureAndRemovePendingCallWhenDocuments(new DataException("No network detected. List operation is not supported offline."), result);
            return result;
        }
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                getTokenAndCallCosmosDbApi(
                        partition,
                        result,
                        new TokenExchangeServiceCallback(mTokenManager) {

                            @Override
                            public void callCosmosDb(TokenResult tokenResult) {
                                callCosmosDbListApi(tokenResult, result, documentType);
                            }

                            @Override
                            public void completeFuture(Exception e) {
                                completeFutureAndRemovePendingCallWhenDocuments(e, result);
                            }
                        });
            }
        }, result, null);
        return result;
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized void instanceCreateOrUpdate(
            final PendingOperation pendingOperation) {
        getTokenAndCallCosmosDbApi(
                Utils.removeAccountIdFromPartitionName(pendingOperation.getPartition()),
                null,
                new TokenExchangeServiceCallback(mTokenManager) {

                    @Override
                    public void callCosmosDb(final TokenResult tokenResult) {
                        callCosmosDbCreateOrUpdateApi(tokenResult, pendingOperation);
                    }

                    @MainThread
                    @Override
                    public void completeFuture(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new DataException("Failed to get Cosmos DB token for performing a create or update operation.", e),
                                pendingOperation);
                    }
                });
    }

    @WorkerThread
    private synchronized <T> void callCosmosDbCreateOrUpdateApi(
            final TokenResult tokenResult,
            T document,
            final Class<T> documentType,
            String partition,
            final String documentId,
            final WriteOptions writeOptions,
            final Map<String, String> additionalHeaders,
            final DefaultAppCenterFuture<DocumentWrapper<T>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                new DocumentWrapper<>(document, partition, documentId).getJsonValue(),
                additionalHeaders,
                new ServiceCallback() {

                    @MainThread
                    @Override
                    public void onCallSucceeded(final String payload, Map<String, String> headers) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                DocumentWrapper<T> cosmosDbDocument = Utils.parseDocument(payload, documentType);
                                if (cosmosDbDocument.hasFailed()) {
                                    completeFutureOnDocumentError(cosmosDbDocument, result);
                                } else {
                                    completeFuture(cosmosDbDocument, result);
                                    mLocalDocumentStorage.writeOnline(Utils.getTableName(tokenResult), cosmosDbDocument, writeOptions);
                                }
                            }
                        });
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    private synchronized void callCosmosDbCreateOrUpdateApi(
            final TokenResult tokenResult,
            final PendingOperation pendingOperation) {
        String outgoingId = Utils.getOutgoingId(pendingOperation.getPartition(), pendingOperation.getDocumentId());
        mOutgoingPendingOperationCalls.put(outgoingId, CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                pendingOperation.getDocument(),
                pendingOperation.getOperation().equals(Constants.PENDING_OPERATION_CREATE_VALUE) ? null : CosmosDb.getUpsertAdditionalHeader(),
                new ServiceCallback() {

                    @MainThread
                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        notifyListenerAndUpdateOperationOnSuccess(payload, pendingOperation);
                    }

                    @MainThread
                    @Override
                    public void onCallFailed(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new DataException("Failed to call Cosmos create or replace API", e),
                                pendingOperation);
                    }
                }));
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized <T> AppCenterFuture<DocumentWrapper<T>> instanceCreateOrUpdate(
            final String documentId, final T document, final Class<T> documentType, final String partition,
            final WriteOptions writeOptions, final Map<String, String> additionalHeaders) {
        final DefaultAppCenterFuture<DocumentWrapper<T>> result = new DefaultAppCenterFuture<>();
        if (isInvalidPartition(partition, result)) {
            return result;
        }
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                if (mNetworkStateHelper.isNetworkConnected()) {
                    getTokenAndCallCosmosDbApi(
                            partition,
                            result,
                            new TokenExchangeServiceCallback(mTokenManager) {

                                @Override
                                public void callCosmosDb(TokenResult tokenResult) {
                                    callCosmosDbCreateOrUpdateApi(tokenResult, document, documentType, tokenResult.getPartition(), documentId, writeOptions, additionalHeaders, result);
                                }

                                @Override
                                public void completeFuture(Exception e) {
                                    Data.this.completeFuture(e, result);
                                }
                            });
                } else {
                    DocumentWrapper<T> createdOrUpdatedDocument;
                    TokenResult cachedToken = getCachedToken(partition);
                    if (cachedToken != null) {
                        String table = Utils.getTableName(cachedToken);
                        createdOrUpdatedDocument = mLocalDocumentStorage.createOrUpdateOffline(table, cachedToken.getPartition(), documentId, document, documentType, writeOptions);
                    } else {
                        createdOrUpdatedDocument = new DocumentWrapper<>(new DataException("Unable to find partition named " + partition + "."));
                    }
                    result.complete(createdOrUpdatedDocument);
                }
            }
        }, result, null);
        return result;
    }

    private synchronized void instanceDelete(final PendingOperation pendingOperation) {
        getTokenAndCallCosmosDbApi(
                Utils.removeAccountIdFromPartitionName(pendingOperation.getPartition()),
                null,
                new TokenExchange.TokenExchangeServiceCallback(mTokenManager) {

                    @Override
                    public void callCosmosDb(TokenResult tokenResult) {
                        callCosmosDbDeleteApi(tokenResult, pendingOperation);
                    }

                    @MainThread
                    @Override
                    public void completeFuture(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new DataException("Failed to get Cosmos DB token for performing a delete operation.", e),
                                pendingOperation);
                    }
                });
    }

    @WorkerThread
    private synchronized void callCosmosDbDeleteApi(
            final TokenResult tokenResult,
            final String documentId,
            final DefaultAppCenterFuture<DocumentWrapper<Void>> result) {
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbApi(
                tokenResult,
                documentId,
                mHttpClient,
                METHOD_DELETE,
                null,
                new ServiceCallback() {

                    @MainThread
                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                completeFuture(new DocumentWrapper<Void>(), result);
                                mLocalDocumentStorage.deleteOnline(Utils.getTableName(tokenResult), tokenResult.getPartition(), documentId);
                            }
                        });
                    }

                    @Override
                    public void onCallFailed(Exception e) {
                        completeFuture(e, result);
                    }
                });
        mPendingCalls.put(result, cosmosDbCall);
    }

    private synchronized void callCosmosDbDeleteApi(TokenResult tokenResult, final PendingOperation operation) {
        String outgoingId = Utils.getOutgoingId(operation.getPartition(), operation.getDocumentId());
        mOutgoingPendingOperationCalls.put(outgoingId, CosmosDb.callCosmosDbApi(
                tokenResult,
                operation.getDocumentId(),
                mHttpClient,
                METHOD_DELETE,
                null,
                new ServiceCallback() {

                    @MainThread
                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        notifyListenerAndUpdateOperationOnSuccess(payload, operation);
                    }

                    @MainThread
                    @Override
                    public void onCallFailed(Exception e) {
                        notifyListenerAndUpdateOperationOnFailure(
                                new DataException("Failed to call Cosmos delete API", e),
                                operation);
                    }
                })
        );
    }

    synchronized void getTokenAndCallCosmosDbApi(
            String partition,
            DefaultAppCenterFuture result,
            TokenExchangeServiceCallback callback) {
        TokenResult cachedTokenResult = mTokenManager.getCachedToken(partition);
        if (cachedTokenResult != null) {
            callback.callCosmosDb(cachedTokenResult);
        } else {
            ServiceCall tokenExchangeServiceCall =
                    TokenExchange.getDbToken(
                            partition,
                            mHttpClient,
                            mTokenExchangeUrl,
                            mAppSecret,
                            callback);
            if (result != null) {
                mPendingCalls.put(result, tokenExchangeServiceCall);
            }
        }
    }

    private interface CallTemplate<T> {

        boolean needsRemoteOperation(DocumentWrapper<T> cachedDocument);

        DocumentWrapper<T> doOfflineOperation(DocumentWrapper<T> cachedDocument, String table, TokenResult cachedToken);

        void callCosmosDb(TokenResult tokenResult, DefaultAppCenterFuture<DocumentWrapper<T>> result);
    }

    private <T> boolean isInvalidPartition(String partition, DefaultAppCenterFuture<DocumentWrapper<T>> result) {
        boolean isInvalidPartition = !LocalDocumentStorage.isValidPartitionName(partition);
        if (isInvalidPartition) {
            completeFuture(getInvalidPartitionDataException(partition), result);
        }
        return isInvalidPartition;
    }

    private synchronized <T> void completeFuture(T value, DefaultAppCenterFuture<T> future) {
        future.complete(value);
        mPendingCalls.remove(future);
    }

    @WorkerThread
    private synchronized <T> void completeFutureAndSaveToLocalStorage(String table, DocumentWrapper<T> value, DefaultAppCenterFuture<DocumentWrapper<T>> future) {
        future.complete(value);
        mLocalDocumentStorage.writeOnline(table, value, new WriteOptions());
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFuture(Exception e, DefaultAppCenterFuture<DocumentWrapper<T>> future) {
        Utils.logApiCallFailure(e);
        future.complete(new DocumentWrapper<T>(e));
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFutureOnDocumentError(DocumentWrapper<T> doc, DefaultAppCenterFuture<DocumentWrapper<T>> future) {
        AppCenterLog.error(LOG_TAG, "Failed to deserialize document.", doc.getError());
        future.complete(doc);
        mPendingCalls.remove(future);
    }

    private synchronized <T> void completeFutureAndRemovePendingCallWhenDocuments(Exception e, DefaultAppCenterFuture<PaginatedDocuments<T>> future) {
        Utils.logApiCallFailure(e);
        future.complete(new PaginatedDocuments<T>().setCurrentPage(new Page<T>(e)));
        mPendingCalls.remove(future);
    }

    private void notifyListenerAndUpdateOperationOnSuccess(final String cosmosDbResponsePayload, final PendingOperation pendingOperation) {
        post(new Runnable() {

            @Override
            public void run() {
                String eTag = Utils.getEtag(cosmosDbResponsePayload);
                pendingOperation.setETag(eTag);
                pendingOperation.setDocument(cosmosDbResponsePayload);
                DataStoreEventListener eventListener = mEventListener;
                if (eventListener != null) {
                    eventListener.onDataStoreOperationResult(
                            pendingOperation.getOperation(),
                            new DocumentMetadata(
                                    pendingOperation.getPartition(),
                                    pendingOperation.getDocumentId(),
                                    eTag),
                            null);
                }
                if (pendingOperation.getExpirationTime() <= System.currentTimeMillis() || PENDING_OPERATION_DELETE_VALUE.equals(pendingOperation.getOperation())) {

                    /* Remove the document if expiration_time has elapsed or it is a delete operation. */
                    mLocalDocumentStorage.deleteOnline(pendingOperation.getTable(), pendingOperation.getPartition(), pendingOperation.getDocumentId());
                } else {

                    /* Clear the pending_operation column if cosmos Db was updated successfully. */
                    pendingOperation.setOperation(null);
                    mLocalDocumentStorage.updatePendingOperation(pendingOperation);
                }
                mOutgoingPendingOperationCalls.remove(Utils.getOutgoingId(pendingOperation.getPartition(), pendingOperation.getDocumentId()));
            }
        });
    }

    private void notifyListenerAndUpdateOperationOnFailure(final DataException e, final PendingOperation pendingOperation) {
        post(new Runnable() {

            @Override
            public void run() {
                AppCenterLog.error(LOG_TAG, "Remote operation failed", e);
                boolean deleteLocalCopy = false;
                if (e.getCause() instanceof HttpException) {
                    switch (((HttpException) e.getCause()).getStatusCode()) {

                        /* The document was removed on the server. */
                        case 404:
                        case 409:

                            /* Partition and document_id combination is already present in the DB. */
                            deleteLocalCopy = true;
                            break;
                    }
                }
                DataStoreEventListener eventListener = mEventListener;
                if (eventListener != null) {
                    eventListener.onDataStoreOperationResult(
                            pendingOperation.getOperation(),
                            null,
                            e);
                }
                if (deleteLocalCopy || pendingOperation.getExpirationTime() <= System.currentTimeMillis()) {

                    /* Remove the document if document was removed on the server, or expiration_time has elapsed. */
                    mLocalDocumentStorage.deleteOnline(pendingOperation.getTable(), pendingOperation.getPartition(), pendingOperation.getDocumentId());
                }
                mOutgoingPendingOperationCalls.remove(Utils.getOutgoingId(pendingOperation.getPartition(), pendingOperation.getDocumentId()));
            }
        });
    }

    private TokenResult getCachedToken(String partitionName) {
        TokenResult result = mTokenManager.getCachedToken(partitionName, true);
        if (result == null) {
            AppCenterLog.error(LOG_TAG, "Unable to find partition named " + partitionName + ".");
            return null;
        } else {
            return result;
        }
    }

    private <T> boolean isInvalidPartitionWhenDocuments(final String partition, final DefaultAppCenterFuture<PaginatedDocuments<T>> result) {
        boolean invalidPartitionName = !LocalDocumentStorage.isValidPartitionName(partition);
        if (invalidPartitionName) {
            completeFutureAndRemovePendingCallWhenDocuments(getInvalidPartitionDataException(partition), result);
        }
        return invalidPartitionName;
    }
}
