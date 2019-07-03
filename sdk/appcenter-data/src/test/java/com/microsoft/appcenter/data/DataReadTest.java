/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.TokenResult;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

public class DataReadTest extends AbstractDataTest {

    @Before
    public void setUpAuth() {
        setUpAuthContext();
    }

    @Test
    public void readFailsToDeserializeDocumentDoesNotThrow() throws JSONException {

        /* Mock http call to get token. */
        /* Pass incorrect document type to cause serialization failure (document is of type TestDocument). */
        AppCenterFuture<DocumentWrapper<String>> doc = Data.read(DOCUMENT_ID, String.class, DefaultPartitions.USER_DOCUMENTS);

        /* Make cosmos db http call with exchanged token. */
        verifyTokenExchangeFlow(TOKEN_EXCHANGE_USER_PAYLOAD, null);
        verifyCosmosDbFlow(DOCUMENT_ID, METHOD_GET, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);

        /* Get and verify document. Confirm the cache was not touched. */
        assertNotNull(doc);
        verifyNoMoreInteractions(mLocalDocumentStorage);
        DocumentWrapper<String> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertTrue(testCosmosDocument.hasFailed());
    }

    @Test
    public void readUserEndToEndWithNetwork() throws JSONException {
        readEndToEndWithNetwork(USER_DOCUMENTS, TOKEN_EXCHANGE_USER_PAYLOAD);
    }

    @Test
    public void readReadonlyEndToEndWithNetwork() throws JSONException {
        readEndToEndWithNetwork(DefaultPartitions.APP_DOCUMENTS, TOKEN_EXCHANGE_READONLY_PAYLOAD);
    }

    private void readEndToEndWithNetwork(String partition, String tokenExchangePayload) throws JSONException {

        /* Mock http call to get token. */
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(DOCUMENT_ID, TestDocument.class, partition, null);

        /* Make cosmos db http call with exchanged token. */
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, tokenExchangePayload, METHOD_GET, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);
        verifyCosmosDbFlow(DOCUMENT_ID, METHOD_GET, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);

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
    public void readFailedCosmosDbCallFailed() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(DOCUMENT_ID, TestDocument.class, USER_DOCUMENTS, new ReadOptions());
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_GET, null, new Exception("Cosmos db exception."));

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
                CoreMatchers.containsString("Cosmos db exception."));
    }

    @Test
    public void readCosmosDbCallEncodeDocumentId() throws JSONException, UnsupportedEncodingException {
        String documentID = "TestDocument";
        Data.read(documentID, TestDocument.class, USER_DOCUMENTS);
        verifyTokenExchangeFlow(TOKEN_EXCHANGE_USER_PAYLOAD, null);

        /* Verify that document base Uri is properly constructed by CosmosDb.getDocumentBaseUrl method. */
        String expectedUri = String.format("dbs/%s", DATABASE_NAME) + "/" +
                String.format("colls/%s", COLLECTION_NAME) + "/" +
                "docs" + '/' + URLEncoder.encode(documentID, "UTF-8");
        assertEquals(expectedUri, CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, documentID));

        /* Now verify that actual call was properly encoded. */
        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, documentID)),
                eq(METHOD_GET),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                notNull(ServiceCallback.class));
    }

    @Test
    public void readCosmosDbCallEncodeDocumentIdWithDoubleQuote() throws JSONException {
        String documentID = "idWith\"DoubleQuote";
        String encodedDocumentId = "idWith%22DoubleQuote";
        Data.delete(documentID, USER_DOCUMENTS);
        verifyTokenExchangeFlow(TOKEN_EXCHANGE_USER_PAYLOAD, null);

        /* Verify that document base Uri is properly constructed by CosmosDb.getDocumentBaseUrl method. */
        String expectedUri = String.format("dbs/%s", DATABASE_NAME) + "/" +
                String.format("colls/%s", COLLECTION_NAME) + "/" +
                String.format("docs/%s", encodedDocumentId);
        assertEquals(expectedUri, CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, documentID));

        /* Now verify that actual call was properly encoded. */
        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, documentID)),
                eq(METHOD_DELETE),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                notNull(ServiceCallback.class));
    }

    @Test
    public void readFailTokenExchangeReturnsFailedTokenResultPayload() {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(DOCUMENT_ID, TestDocument.class, USER_DOCUMENTS);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verifyNoMoreInteractions(mLocalDocumentStorage);
        verify(mHttpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        String tokenExchangeFailedResponsePayload = "{\n" +
                "    \"tokens\": [\n" +
                "        {\n" +
                "            \"partition\": \"\",\n" +
                "            \"dbAccount\": \"\",\n" +
                "            \"dbName\": \"\",\n" +
                "            \"dbCollectionName\": \"\",\n" +
                "            \"token\": \"\",\n" +
                "            \"status\": \"Failed\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeFailedResponsePayload, new HashMap<String, String>());

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
                CoreMatchers.containsString(tokenExchangeFailedResponsePayload));
    }

    @Test
    public void readTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(DOCUMENT_ID, TestDocument.class, USER_DOCUMENTS);

        String exceptionMessage = "Call to token exchange failed for whatever reason";
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_GET, null, new HttpException(503, exceptionMessage));

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
    public void readWithoutNetwork() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), anyString(), anyString(), eq(TestDocument.class), any(ReadOptions.class))).thenReturn(new DocumentWrapper<TestDocument>(new Exception("document not set")));
        Data.read(DOCUMENT_ID, TestDocument.class, USER_DOCUMENTS);
        verifyNoMoreInteractions(mHttpClient);
        verify(mLocalDocumentStorage).read(
                eq(USER_TABLE_NAME),
                eq(RESOLVED_USER_PARTITION),
                eq(DOCUMENT_ID),
                eq(TestDocument.class),
                any(ReadOptions.class));
    }

    @Test
    public void readWhenLocalStorageContainsDeletePendingOperation() {
        String failedMessage = "The document is found in local storage but marked as state deleted.";
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        DocumentWrapper<String> deletedDocument = new DocumentWrapper<>();
        deletedDocument.setPendingOperation(Constants.PENDING_OPERATION_DELETE_VALUE);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), anyString(), anyString(), eq(String.class), any(ReadOptions.class))).thenReturn(deletedDocument);
        DocumentWrapper<String> document = Data.read(DOCUMENT_ID, String.class, USER_DOCUMENTS).get();
        assertNotNull(document.getError());
        assertTrue(document.getError().getMessage().contains(failedMessage));
    }

    @Test
    public void readOnLineWhenLocalStorageContainsNullOperation() {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        TokenResult tokenResult = new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("tokenResult")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collectionName")
                .setDbName("dbName");
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(Utils.getGson().toJson(tokenResult));
        DocumentWrapper<String> outDatedDocument = new DocumentWrapper<>();
        DocumentWrapper<String> expectedDocument = new DocumentWrapper<>("123", RESOLVED_USER_PARTITION, DOCUMENT_ID);
        final String expectedResponse = Utils.getGson().toJson(expectedDocument);
        when(mLocalDocumentStorage.read(eq(Utils.getTableName(tokenResult)), anyString(), anyString(), eq(String.class), any(ReadOptions.class))).thenReturn(outDatedDocument);
        when(mHttpClient.callAsync(contains(DOCUMENT_ID), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                return mock(ServiceCall.class);
            }
        });
        DocumentWrapper<String> document = Data.read(DOCUMENT_ID, String.class, USER_DOCUMENTS).get();
        verify(mHttpClient);
        assertNotNull(document.getDeserializedValue());
        assertEquals(expectedDocument.getDeserializedValue(), document.getDeserializedValue());
    }

    @Test
    public void readWhenLocalStorageContainsCreateOperation() {
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        DocumentWrapper<String> createdDocument = new DocumentWrapper<>("123", RESOLVED_USER_PARTITION, DOCUMENT_ID);
        createdDocument.setPendingOperation(Constants.PENDING_OPERATION_CREATE_VALUE);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), anyString(), anyString(), eq(String.class), any(ReadOptions.class))).thenReturn(createdDocument);
        DocumentWrapper<String> document = Data.read(DOCUMENT_ID, String.class, USER_DOCUMENTS).get();
        verifyNoMoreInteractions(mHttpClient);
        assertEquals(createdDocument.getDeserializedValue(), document.getDeserializedValue());
    }
}
