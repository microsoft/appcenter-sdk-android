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

import com.google.gson.JsonElement;
import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.client.TokenExchange.TokenExchangeServiceCallback;
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentMetadata;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.NextPageDelegate;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.RemoteOperationListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final HashMap<String, ServiceCall> mOutgoingPendingOperationCalls = new HashMap<>();

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Current token exchange base URL.
     */
    private String mTokenExchangeUrl = DEFAULT_API_URL;

    private final Map<DefaultAppCenterFuture<?>, ServiceCall> mPendingCalls = new HashMap<>();

    private HttpClient mHttpClient;

    private TokenManager mTokenManager;

    private LocalDocumentStorage mLocalDocumentStorage;

    /**
     * Current remote operation listener.
     */
    private volatile RemoteOperationListener mRemoteOperationListener;

    /**
     * Authorization listener for {@link AuthTokenContext}.
     */
    private AuthTokenContext.Listener mAuthListener;

    private NetworkStateHelper mNetworkStateHelper;

    /**
     * Document ID validation pattern.
     */
    private final Pattern sDocumentIdPattern = Pattern.compile("^[^/\\\\#\\s?]+$");

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
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
    public static void setTokenExchangeUrl(String tokenExchangeUrl) {
        getInstance().setInstanceTokenExchangeUrl(tokenExchangeUrl);
    }

    /**
     * Check whether Data service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Data service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Read a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static <T> AppCenterFuture<DocumentWrapper<T>> read(String documentId, Class<T> documentType, String partition) {
        return read(documentId, documentType, partition, new ReadOptions());
    }

    /**
     * Read a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param readOptions  Cache read options when the operation is done offline.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static <T> AppCenterFuture<DocumentWrapper<T>> read(String documentId, Class<T> documentType, String partition, ReadOptions readOptions) {
        return getInstance().instanceRead(documentId, documentType, partition, ReadOptions.ensureNotNull(readOptions));
    }

    /**
     * Retrieve a paginated list of the documents in a partition.
     *
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document list.
     * If the operation fails, the error can be checked by reading {@link Page#getError()} on the first page of the results: {@link PaginatedDocuments#getCurrentPage()}.
     */
    public static <T> AppCenterFuture<PaginatedDocuments<T>> list(Class<T> documentType, String partition) {
        return list(documentType, partition, new ReadOptions());
    }

    /**
     * Retrieve a paginated list of the documents in a partition.
     *
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param readOptions  Cache read options when the operation is done offline.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document list.
     * If the operation fails, the error can be checked by reading {@link Page#getError()} on the first page of the results: {@link PaginatedDocuments#getCurrentPage()}.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static <T> AppCenterFuture<PaginatedDocuments<T>> list(Class<T> documentType, String partition, ReadOptions readOptions) {
        return getInstance().instanceList(documentType, partition, ReadOptions.ensureNotNull(readOptions));
    }

    /**
     * Create a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param document     The document.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static <T> AppCenterFuture<DocumentWrapper<T>> create(String documentId, T document, Class<T> documentType, String partition) {
        return create(documentId, document, documentType, partition, new WriteOptions());
    }

    /**
     * Create a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param document     The document.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param writeOptions Cache write options when the operation is done offline.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static <T> AppCenterFuture<DocumentWrapper<T>> create(String documentId, T document, Class<T> documentType, String partition, WriteOptions writeOptions) {
        return getInstance().instanceCreateOrUpdate(documentId, document, documentType, partition, WriteOptions.ensureNotNull(writeOptions), null);
    }

    /**
     * Delete a document.
     *
     * @param documentId The CosmosDB document id.
     * @param partition  The CosmosDB partition key.
     * @return Future asynchronous operation with result being the document metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static AppCenterFuture<DocumentWrapper<Void>> delete(String documentId, String partition) {
        return delete(documentId, partition, new WriteOptions());
    }

    /**
     * Delete a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param partition    The CosmosDB partition key.
     * @param writeOptions Cache write options when the operation is done offline.
     * @return Future asynchronous operation with result being the document metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static AppCenterFuture<DocumentWrapper<Void>> delete(String documentId, String partition, WriteOptions writeOptions) {
        return getInstance().instanceDelete(documentId, partition, WriteOptions.ensureNotNull(writeOptions));
    }

    /**
     * Replace a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param document     The document.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    public static <T> AppCenterFuture<DocumentWrapper<T>> replace(String documentId, T document, Class<T> documentType, String partition) {
        return replace(documentId, document, documentType, partition, new WriteOptions());
    }

    /**
     * Replace a document.
     *
     * @param documentId   The CosmosDB document id.
     * @param document     The document.
     * @param documentType The document type.
     * @param partition    The CosmosDB partition key.
     * @param writeOptions Cache write options when the operation is done offline.
     * @param <T>          The document type.
     * @return Future asynchronous operation with result being the document with metadata.
     * If the operation fails, the error can be checked by reading {@link DocumentWrapper#getError()}.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static <T> AppCenterFuture<DocumentWrapper<T>> replace(String documentId, T document, Class<T> documentType, String partition, WriteOptions writeOptions) {
        return getInstance().instanceCreateOrUpdate(documentId, document, documentType, partition, WriteOptions.ensureNotNull(writeOptions), CosmosDb.getUpsertAdditionalHeader());
    }

    /**
     * Sets a listener that will be invoked on network status change to notify of pending operations execution status.
     * Pass null to unregister.
     *
     * @param listener listener to register or null to unregister a previous listener.
     */
    public static void setRemoteOperationListener(RemoteOperationListener listener) {
        getInstance().mRemoteOperationListener = listener;
    }

    private static DataException getInvalidPartitionDataException(String partition) {
        return new DataException(String.format("Partition name can be either '%s' or '%s' but not '%s'.", APP_DOCUMENTS, USER_DOCUMENTS, partition));
    }

    private static IllegalStateException getModuleNotStartedException() {
        return new IllegalStateException("Data module is either disabled or has not been started. Add `Data.class` to the `AppCenter.start(...)` call.");
    }

    /**
     * Implements {@link #setTokenExchangeUrl(String)}}.
     */
    private synchronized void setInstanceTokenExchangeUrl(String tokenExchangeUrl) {
        mTokenExchangeUrl = tokenExchangeUrl;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mNetworkStateHelper = NetworkStateHelper.getSharedInstance(context);
        mHttpClient = createHttpClient(context, false);
        mTokenManager = TokenManager.getInstance(context);
        mAppSecret = appSecret;
        mLocalDocumentStorage = new LocalDocumentStorage(context, Utils.getUserTableName());
        mAuthListener = new AbstractTokenContextListener() {

            @Override
            public void onNewUser(String accountId) {
                if (accountId == null) {
                    mTokenManager.removeAllCachedTokens();
                    mLocalDocumentStorage.resetDatabase();
                } else {
                    String userTable = Utils.getUserTableName(accountId);
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
        for (LocalDocument localDocument : mLocalDocumentStorage.getPendingOperations(Utils.getUserTableName())) {
            String outgoingId = Utils.getOutgoingId(localDocument.getPartition(), localDocument.getDocumentId());

            /* If the operation is already being processed, skip it. */
            if (mOutgoingPendingOperationCalls.containsKey(outgoingId)) {
                continue;
            }

            /* Put the pending document id into the map to prevent further duplicate http call. The ServiceCall will be set when the http operation executes. */
            mOutgoingPendingOperationCalls.put(outgoingId, null);
            if (PENDING_OPERATION_CREATE_VALUE.equals(localDocument.getOperation()) ||
                    PENDING_OPERATION_REPLACE_VALUE.equals(localDocument.getOperation())) {
                instanceCreateOrUpdate(localDocument);
            } else if (PENDING_OPERATION_DELETE_VALUE.equals(localDocument.getOperation())) {
                instanceDelete(localDocument);
            } else {
                AppCenterLog.debug(LOG_TAG, String.format("Pending operation '%s' is not supported.", localDocument.getOperation()));
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
        if (isInvalidStateOrParameters(partition, documentId, result)) {
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
        }, result, new DocumentWrapper<T>(getModuleNotStartedException()));
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
                    success = mLocalDocumentStorage.deleteOffline(table, cachedDocument, writeOptions);
                } else {
                    success = mLocalDocumentStorage.deleteOnline(table, cachedToken.getPartition(), documentId);
                }
                if (success) {
                    return cachedDocument;
                } else {
                    return new DocumentWrapper<>(new DataException("Failed to write to cache."));
                }
            }

            @Override
            public void callCosmosDb(TokenResult tokenResult, DefaultAppCenterFuture<DocumentWrapper<Void>> result) {
                callCosmosDbDeleteApi(tokenResult, partition, documentId, result);
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
            final ReadOptions readOptions,
            final Class<T> documentType,
            final String continuationToken) {
        if (continuationToken != null) {
            if (!mNetworkStateHelper.isNetworkConnected()) {

                /* If not online, return an error. */
                completeFutureAndRemovePendingCallWhenDocuments(new DataException("Listing next page is not supported in off-line mode."), result);
                return;
            }
        }
        ServiceCall cosmosDbCall = CosmosDb.callCosmosDbListApi(
                tokenResult,
                continuationToken,
                mHttpClient,
                new ServiceCallback() {

                    @Override
                    public void onCallSucceeded(String payload, Map<String, String> headers) {
                        Page<T> page = Utils.parseDocuments(payload, documentType);
                        String tableName = Utils.getTableName(tokenResult);
                        List<DocumentWrapper<T>> items = page.getItems();
                        if (items != null) {
                            for (DocumentWrapper<T> document : items) {
                                if (document.getError() == null) {
                                    mLocalDocumentStorage.writeOnline(tableName, document, new WriteOptions(readOptions.getDeviceTimeToLive()));
                                }
                            }
                        }
                        PaginatedDocuments<T> paginatedDocuments = new PaginatedDocuments<T>()
                                .setCurrentPage(page).setTokenResult(tokenResult)
                                .setContinuationToken(headers.get(Constants.CONTINUATION_TOKEN_HEADER))
                                .setReadOptions(readOptions)
                                .setDocumentType(documentType)
                                .setNextPageDelegate(new NextPageDelegate() {
                                    
                                    @Override
                                    public <TDocument> void loadNextPage(
                                            TokenResult tokenResult,
                                            DefaultAppCenterFuture<PaginatedDocuments<TDocument>> result,
                                            ReadOptions readOptions,
                                            Class<TDocument> documentType,
                                            String continuationToken) {
                                        Data.this.callCosmosDbListApi(
                                                tokenResult,
                                                result,
                                                readOptions,
                                                documentType,
                                                continuationToken);
                                    }
                                });
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
    private synchronized <T> AppCenterFuture<PaginatedDocuments<T>> instanceList(
            final Class<T> documentType,
            final String partition,
            final ReadOptions readOptions) {
        final DefaultAppCenterFuture<PaginatedDocuments<T>> result = new DefaultAppCenterFuture<>();
        if (isInvalidStateOrParametersWhenDocuments(partition, result)) {
            return result;
        }
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                String tableName = Utils.getTableName(partition);
                if (tableName == null) {
                    completeFutureAndRemovePendingCallWhenDocuments(new DataException("List operation requested on user partition, but the user is not logged in."), result);
                    return;
                }
                final TokenResult cachedTokenResult = mTokenManager.getCachedToken(partition, true);
                if (cachedTokenResult == null && !mNetworkStateHelper.isNetworkConnected()) {
                    completeFutureAndRemovePendingCallWhenDocuments(new DataException("List operation requested on a partition, but no network."), result);
                    return;
                }
                List<LocalDocument> localDocuments;
                if (cachedTokenResult != null) {
                    localDocuments = mLocalDocumentStorage.getDocumentsByPartition(tableName, cachedTokenResult.getPartition(), readOptions);
                    if (LocalDocumentStorage.hasPendingOperation(localDocuments)) {
                        completeFuture(Utils.localDocumentsToNonExpiredPaginated(localDocuments, documentType), result);
                        return;
                    }
                } else {
                    localDocuments = new ArrayList<>();
                }
                if (!mNetworkStateHelper.isNetworkConnected()) {
                    completeFuture(Utils.localDocumentsToNonExpiredPaginated(localDocuments, documentType), result);
                    return;
                }
                getTokenAndCallCosmosDbApi(
                        partition,
                        result,
                        new TokenExchangeServiceCallback(mTokenManager) {

                            @Override
                            public void callCosmosDb(TokenResult tokenResult) {
                                callCosmosDbListApi(tokenResult, result, readOptions, documentType, null);
                            }

                            @Override
                            public void completeFuture(Exception e) {
                                completeFutureAndRemovePendingCallWhenDocuments(e, result);
                            }
                        });
            }
        }, result, new PaginatedDocuments<T>().setCurrentPage(new Page<T>(getModuleNotStartedException())));
        return result;
    }

    /**
     * Create a document.
     * The document type (T) must be JSON deserializable.
     */
    private synchronized void instanceCreateOrUpdate(
            final LocalDocument pendingOperation) {
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
                new DocumentWrapper<>(document, partition, documentId).toString(),
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
            final LocalDocument pendingOperation) {
        String outgoingId = Utils.getOutgoingId(pendingOperation.getPartition(), pendingOperation.getDocumentId());

        /* Build payload. */
        JsonElement documentPayload = Utils.getGson().fromJson(pendingOperation.getDocument(), JsonElement.class);
        DocumentWrapper<JsonElement> documentWrapper = new DocumentWrapper<>(documentPayload, pendingOperation.getPartition(), pendingOperation.getDocumentId(), pendingOperation.getETag(), 0);
        mOutgoingPendingOperationCalls.put(outgoingId, CosmosDb.callCosmosDbApi(
                tokenResult,
                null,
                mHttpClient,
                METHOD_POST,
                documentWrapper.toString(),
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
        if (isInvalidStateOrParameters(partition, documentId, result)) {
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
        }, result, new DocumentWrapper<T>(getModuleNotStartedException()));
        return result;
    }

    private synchronized void instanceDelete(final LocalDocument pendingOperation) {
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
            final String partition,
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
                                DocumentWrapper<Void> wrapper = new DocumentWrapper<>(null, partition, documentId);
                                completeFuture(wrapper, result);
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

    private synchronized void callCosmosDbDeleteApi(TokenResult tokenResult, final LocalDocument operation) {
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

    private void notifyListenerAndUpdateOperationOnSuccess(final String cosmosDbResponsePayload, final LocalDocument pendingOperation) {
        post(new Runnable() {

            @Override
            public void run() {
                String eTag = Utils.getETag(cosmosDbResponsePayload);
                pendingOperation.setETag(eTag);
                RemoteOperationListener eventListener = mRemoteOperationListener;
                if (eventListener != null) {
                    eventListener.onRemoteOperationCompleted(
                            pendingOperation.getOperation(),
                            new DocumentMetadata(
                                    pendingOperation.getPartition(),
                                    pendingOperation.getDocumentId(),
                                    eTag),
                            null);
                }
                if (pendingOperation.isExpired() || PENDING_OPERATION_DELETE_VALUE.equals(pendingOperation.getOperation())) {

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

    private void notifyListenerAndUpdateOperationOnFailure(final DataException e, final LocalDocument pendingOperation) {
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
                RemoteOperationListener eventListener = mRemoteOperationListener;
                if (eventListener != null) {
                    eventListener.onRemoteOperationCompleted(
                            pendingOperation.getOperation(),
                            null,
                            e);
                }
                if (deleteLocalCopy || pendingOperation.isExpired()) {

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

    private <T> boolean isInvalidStateOrParameters(String partition, String documentId, DefaultAppCenterFuture<DocumentWrapper<T>> result) {
        boolean isNotStarted = mAppSecret == null;
        if (isNotStarted) {
            completeFuture(getModuleNotStartedException(), result);
            return true;
        }
        boolean isValidPartition = LocalDocumentStorage.isValidPartitionName(partition);
        if (!isValidPartition) {
            completeFuture(getInvalidPartitionDataException(partition), result);
            return true;
        }
        boolean isValidDocumentId = isValidDocumentId(documentId);
        if (!isValidDocumentId) {
            completeFuture(new DataException("Invalid document ID."), result);
            return true;
        }
        return false;
    }

    private <T> boolean isInvalidStateOrParametersWhenDocuments(final String partition, final DefaultAppCenterFuture<PaginatedDocuments<T>> result) {
        boolean isNotStarted = mAppSecret == null;
        if (isNotStarted) {
            completeFutureAndRemovePendingCallWhenDocuments(getModuleNotStartedException(), result);
            return true;
        }
        boolean invalidPartitionName = !LocalDocumentStorage.isValidPartitionName(partition);
        if (invalidPartitionName) {
            completeFutureAndRemovePendingCallWhenDocuments(getInvalidPartitionDataException(partition), result);
            return true;
        }
        return false;
    }

    private boolean isValidDocumentId(String documentId) {
        if (documentId == null) {
            return false;
        }
        Matcher matcher = sDocumentIdPattern.matcher(documentId);
        return matcher.matches();
    }

    private interface CallTemplate<T> {

        boolean needsRemoteOperation(DocumentWrapper<T> cachedDocument);

        DocumentWrapper<T> doOfflineOperation(DocumentWrapper<T> cachedDocument, String table, TokenResult cachedToken);

        void callCosmosDb(TokenResult tokenResult, DefaultAppCenterFuture<DocumentWrapper<T>> result);
    }
}
