/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.TokenResult;
import com.microsoft.appcenter.data.models.WriteOptions;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.SSLException;

import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

public class DataCreateUpdateDeleteTest extends AbstractDataTest {

    @Before
    public void setUpAuth() {
        setUpAuthContext();
    }

    @Test
    public void replaceEndToEnd() throws JSONException {

        /* Mock http call to get token. */
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.replace(DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS, null);

        /* Make the call. */
        verifyTokenExchangeToCosmosDbFlow(null, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_POST, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);

        /* Get and verify token. */
        assertNotNull(doc);

        DocumentWrapper<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertEquals(RESOLVED_USER_PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getETag());
        assertNotEquals(0L, testCosmosDocument.getLastUpdatedDate());

        TestDocument testDocument = testCosmosDocument.getDeserializedValue();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void createFailsToDeserializeDocumentDoesNotThrow() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, DefaultPartitions.USER_DOCUMENTS, new WriteOptions());

        /* Mock for cosmos return payload that cannot be deserialized. Will fail in the `onSuccess` callback of `create`. */
        verifyTokenExchangeToCosmosDbFlow(null, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_POST, "", null);

        /* Verify document error. Confirm the cache was not touched. */
        assertNotNull(doc);
        verifyNoMoreInteractions(mLocalDocumentStorage);
        DocumentWrapper<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertTrue(testCosmosDocument.hasFailed());
    }

    @Test
    public void createEndToEndWithNetwork() throws JSONException {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        WriteOptions writeOptions = new WriteOptions(12476);
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS, writeOptions);
        verifyTokenExchangeToCosmosDbFlow(null, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_POST, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);
        assertNotNull(doc);
        DocumentWrapper<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        verify(mLocalDocumentStorage, times(1)).writeOnline(eq(USER_TABLE_NAME), refEq(testCosmosDocument), refEq(writeOptions));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        assertEquals(RESOLVED_USER_PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getETag());
        assertNotEquals(0L, testCosmosDocument.getLastUpdatedDate());

        TestDocument testDocument = testCosmosDocument.getDeserializedValue();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void createPendingOperationWithoutUpsertHeader() {

        /* If we have one pending operation delete, and the network is on. */
        final LocalDocument pendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_CREATE_VALUE,
                RESOLVED_USER_PARTITION,
                DOCUMENT_ID,
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Collections.singletonList(pendingOperation));

        /* Setup mock to get valid token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setDbName("db")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collection")
                .setToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        mockStatic(CosmosDb.class);
        when(CosmosDb.getUpsertAdditionalHeader()).thenReturn(new HashMap<String, String>() {
            {
                put("abc", "bcd");
            }
        });

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(CosmosDb.callCosmosDbApi(any(TokenResult.class),
                anyString(), any(HttpClient.class), anyString(), anyString(), headers.capture(), any(ServiceCallback.class))).then(
                new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[6]).onCallFailed(new HttpException(409, "Conflict happened."));
                        return mock(ServiceCall.class);
                    }
                });

        /* Set up listener. */
        Data.unsetInstance();
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify additional headers does not contain the upsert key. */
        assertNull(headers.getValue());
    }

    @Test
    public void replacePendingOperationWithUpsertHeader() {

        /* If we have one pending operation delete, and the network is on. */
        final LocalDocument pendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
                RESOLVED_USER_PARTITION,
                DOCUMENT_ID,
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Collections.singletonList(pendingOperation));

        /* Setup mock to get valid token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setDbName("db")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collection")
                .setToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        mockStatic(CosmosDb.class);
        when(CosmosDb.getUpsertAdditionalHeader()).thenReturn(new HashMap<String, String>() {
            {
                put("abc", "bcd");
            }
        });

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(CosmosDb.callCosmosDbApi(any(TokenResult.class),
                anyString(), any(HttpClient.class), anyString(), anyString(), headers.capture(), any(ServiceCallback.class))).then(
                new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[6]).onCallFailed(new HttpException(409, "Conflict happened."));
                        return mock(ServiceCall.class);
                    }
                });

        /* Set up listener. */
        Data.unsetInstance();
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify additional headers is not null so it contains upsert key. */
        assertNotNull(headers.getValue());
    }

    @Test
    public void createWithoutUpsertHeader() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Setup mock to get valid token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setDbName("db")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collection")
                .setToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        mockStatic(CosmosDb.class);
        when(CosmosDb.getUpsertAdditionalHeader()).thenReturn(new HashMap<String, String>() {
            {
                put("abc", "bcd");
            }
        });

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(CosmosDb.callCosmosDbApi(any(TokenResult.class),
                anyString(), any(HttpClient.class), anyString(), anyString(), headers.capture(), any(ServiceCallback.class))).then(
                new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[6]).onCallFailed(new HttpException(409, "Conflict happened."));
                        return mock(ServiceCall.class);
                    }
                });

        /* Create the document. */
        Data.create("123", "test", String.class, USER_DOCUMENTS).get();

        /* Verify the additional header does not contain upsert key. */
        assertNull(headers.getValue());
    }

    @Test
    public void replaceWithUpsertHeader() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Setup mock to get valid token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setDbName("db")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collection")
                .setToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        mockStatic(CosmosDb.class);
        when(CosmosDb.getUpsertAdditionalHeader()).thenReturn(new HashMap<String, String>() {
            {
                put("abc", "bcd");
            }
        });

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(CosmosDb.callCosmosDbApi(any(TokenResult.class),
                anyString(), any(HttpClient.class), anyString(), anyString(), headers.capture(), any(ServiceCallback.class))).then(
                new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[6]).onCallSucceeded("", new HashMap<String, String>());
                        return mock(ServiceCall.class);
                    }
                });

        /* Replace the document. */
        Data.replace("123", "test", String.class, USER_DOCUMENTS).get();

        /* Verify the additional header does not contain upsert key. */
        assertNotNull(headers.getValue());
    }

    @Test
    public void createWithNoNetwork() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        TestDocument testDocument = new TestDocument("test");
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        Data.create(DOCUMENT_ID, testDocument, TestDocument.class, USER_DOCUMENTS);
        verifyNoMoreInteractions(mHttpClient);
        verify(mLocalDocumentStorage).createOrUpdateOffline(
                eq(USER_TABLE_NAME),
                eq(RESOLVED_USER_PARTITION),
                eq(DOCUMENT_ID),
                eq(testDocument),
                eq(TestDocument.class),
                any(WriteOptions.class)
        );
    }

    @Test
    public void createTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DOCUMENT_ID, new TestDocument("test"), TestDocument.class, USER_DOCUMENTS);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        verifyTokenExchangeFlow(null, new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDeserializedValue());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void createCosmosDbCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DOCUMENT_ID, new TestDocument("test"), TestDocument.class, USER_DOCUMENTS);
        String exceptionMessage = "Call to Cosmos DB failed for whatever reason";
        verifyTokenExchangeToCosmosDbFlow(null, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_POST, null, new Exception(exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDeserializedValue());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void deleteEndToEnd() throws JSONException {
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(RESOLVED_USER_PARTITION), eq(DOCUMENT_ID), eq(Void.class), notNull(ReadOptions.class))).thenReturn(new DocumentWrapper<Void>(new DataException("not found")));
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mLocalDocumentStorage).deleteOnline(eq(USER_TABLE_NAME), eq(RESOLVED_USER_PARTITION), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        DocumentWrapper<Void> wrapper = doc.get();
        assertNotNull(wrapper);
        assertEquals(USER_DOCUMENTS, wrapper.getPartition());
        assertEquals(DOCUMENT_ID, wrapper.getId());
        assertFalse(wrapper.hasFailed());
        assertNull(wrapper.getError());
    }

    @Test
    public void deleteTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        verifyTokenExchangeFlow(null, new SSLException(exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDeserializedValue());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void deleteCosmosDbCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        String exceptionMessage = "Call to Cosmos DB failed for whatever reason";
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, null, new HttpException(400, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDeserializedValue());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void deleteWithoutNetworkSucceeds() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        DocumentWrapper<Void> cachedDocument = new DocumentWrapper<>(null, DefaultPartitions.USER_DOCUMENTS, DOCUMENT_ID, "someETag", System.currentTimeMillis() + 6000);
        WriteOptions writeOptions = new WriteOptions();
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(cachedDocument);
        when(mLocalDocumentStorage.deleteOffline(eq(USER_TABLE_NAME), eq(cachedDocument), refEq(writeOptions))).thenReturn(true);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        verify(mLocalDocumentStorage).deleteOffline(eq(USER_TABLE_NAME), eq(cachedDocument), refEq(writeOptions));
        verify(mLocalDocumentStorage, never()).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertNull(result.get().getError());
    }

    @Test
    public void deleteWithoutNetworkFails() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        WriteOptions writeOptions = new WriteOptions(1234);
        DocumentWrapper<Void> cachedDocument = new DocumentWrapper<>(null, DefaultPartitions.USER_DOCUMENTS, DOCUMENT_ID, "someETag", System.currentTimeMillis() + 6000);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(cachedDocument);
        when(mLocalDocumentStorage.deleteOffline(eq(USER_TABLE_NAME), anyString(), anyString(), eq(writeOptions))).thenReturn(false);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(DOCUMENT_ID, USER_DOCUMENTS, writeOptions);
        verify(mLocalDocumentStorage).deleteOffline(eq(USER_TABLE_NAME), refEq(cachedDocument), eq(writeOptions));
        verify(mLocalDocumentStorage, never()).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(result.get().getError());
    }

    @Test
    public void deleteSuccessfullyFromLocalStorageWithoutNetworkCallWhenDocumentCreatedOnlyOffline() {
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(new DocumentWrapper<Void>());
        when(mLocalDocumentStorage.deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID))).thenReturn(true);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        verify(mLocalDocumentStorage).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertFalse(result.get().hasFailed());
        assertNull(result.get().getError());
    }

    @Test
    public void failToDeleteFromLocalStorageWithoutNetworkCallWhenDocumentCreatedOnlyOffline() {
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(new DocumentWrapper<Void>());
        when(mLocalDocumentStorage.deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID))).thenReturn(false);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(DOCUMENT_ID, USER_DOCUMENTS);
        verify(mLocalDocumentStorage).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertTrue(result.get().hasFailed());
        assertNotNull(result.get().getError());
    }
}
