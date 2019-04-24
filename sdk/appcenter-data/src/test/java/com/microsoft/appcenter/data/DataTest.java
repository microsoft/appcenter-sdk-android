/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.google.gson.JsonSyntaxException;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
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
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.SSLException;

import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        CosmosDb.class,
        TokenExchange.class,
        TokenManager.class
})
public class DataTest extends AbstractDataTest {

    @Mock
    private DataStoreEventListener mDataStoreEventListener;

    private static final String TOKEN_RESULT = String.format("{\n" +
            "            \"partition\": \"%s\",\n" +
            "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
            "            \"dbName\": \"db\",\n" +
            "            \"dbCollectionName\": \"collection\",\n" +
            "            \"token\": \"%s\",\n" +
            "            \"status\": \"Succeed\",\n" +
            "            \"accountId\": \"%s\"\n" +
            "}", RESOLVED_USER_PARTITION, "token", ACCOUNT_ID);

    @Before
    public void setUpAuth() {
        setUpAuthContext();
    }

    @Test
    public void singleton() {
        Assert.assertSame(Data.getInstance(), Data.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Data.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Data.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {
        Data.setEnabled(true);

        verify(mChannel).removeGroup(eq(mData.getGroupName()));
        verify(mChannel).addGroup(eq(mData.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Data.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Data.setEnabled(false).get();
        assertFalse(Data.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(DATA_ENABLED_KEY, true)).thenReturn(false);
        verify(mChannel, never()).removeListener(any(Channel.Listener.class));
        verify(mChannel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void listWhenOffline() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(USER_DOCUMENTS, TestDocument.class).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        Page<TestDocument> page = docs.getCurrentPage();
        assertNull(page.getItems());
        assertNotNull(page.getError());
        assertEquals(DataException.class, page.getError().getClass());
        verifyZeroInteractions(mHttpClient);
        verifyZeroInteractions(mDataStoreEventListener);
        verifyZeroInteractions(mLocalDocumentStorage);
        verifyZeroInteractions(mAuthTokenContext);
    }

    @Test
    public void listEndToEndWhenSinglePage() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<DocumentWrapper<TestDocument>> documents = Collections.singletonList(new DocumentWrapper<>(
                new TestDocument("Test"),
                RESOLVED_USER_PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(documents)
        );
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(USER_DOCUMENTS, TestDocument.class).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertEquals(1, docs.getCurrentPage().getItems().size());
        assertEquals(docs.getCurrentPage().getItems().get(0).getDeserializedValue().test, documents.get(0).getDeserializedValue().test);

        /* Disable the Data module. */
        Data.setEnabled(false).get();

        /* Make the list call again. */
        PaginatedDocuments<TestDocument> docCancel = Data.list(USER_DOCUMENTS, TestDocument.class).get();
        assertNull(docCancel);
    }

    @Test
    public void listEndToEndWhenMultiplePages() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<DocumentWrapper<TestDocument>> firstPartDocuments = Collections.singletonList(new DocumentWrapper<>(
                new TestDocument("Test"),
                RESOLVED_USER_PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedFirstResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(firstPartDocuments)
        );
        final List<DocumentWrapper<TestDocument>> secondPartDocuments = Collections.singletonList(new DocumentWrapper<>(
                new TestDocument("Test2"),
                RESOLVED_USER_PARTITION,
                "document id 2",
                "e tag 2",
                1
        ));
        final String expectedSecondResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(secondPartDocuments)
        );

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), headers.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                String expectedResponse = headers.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? expectedSecondResponse : expectedFirstResponse;
                Map<String, String> newHeader = headers.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? new HashMap<String, String>() : new HashMap<String, String>() {
                    {
                        put(Constants.CONTINUATION_TOKEN_HEADER, "continuation token");
                    }
                };
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, newHeader);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(USER_DOCUMENTS, TestDocument.class).get();
        assertTrue(docs.hasNextPage());
        assertEquals(firstPartDocuments.get(0).getId(), docs.getCurrentPage().getItems().get(0).getId());
        Page<TestDocument> secondPage = docs.getNextPage().get();
        assertFalse(docs.hasNextPage());
        assertEquals(secondPage.getItems().get(0).getId(), docs.getCurrentPage().getItems().get(0).getId());
    }

    @Test
    public void listEndToEndWhenUseIterators() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<DocumentWrapper<TestDocument>> firstPartDocuments = Collections.nCopies(2, new DocumentWrapper<>(
                new TestDocument("Test"),
                RESOLVED_USER_PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedFirstResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(firstPartDocuments)
        );
        final List<DocumentWrapper<TestDocument>> secondPartDocuments = Collections.singletonList(new DocumentWrapper<>(
                new TestDocument("Test2"),
                RESOLVED_USER_PARTITION,
                "document id 2",
                "e tag 2",
                1
        ));
        final String expectedSecondResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(secondPartDocuments)
        );

        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass((Class) Map.class);
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), headers.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                String expectedResponse = headers.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? expectedSecondResponse : expectedFirstResponse;
                Map<String, String> newHeader = headers.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? new HashMap<String, String>() : new HashMap<String, String>() {
                    {
                        put(Constants.CONTINUATION_TOKEN_HEADER, "continuation token");
                    }
                };
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, newHeader);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        Iterator<DocumentWrapper<TestDocument>> iterator = Data.list(USER_DOCUMENTS, TestDocument.class).get().iterator();
        List<DocumentWrapper<TestDocument>> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertEquals(3, documents.size());
        assertEquals(firstPartDocuments.get(0).getId(), documents.get(0).getId());
        assertEquals(secondPartDocuments.get(0).getId(), documents.get(2).getId());
        assertNotNull(iterator.next().getError());

        /* Verify not throws exception. */
        iterator.remove();
    }

    @Test
    public void listEndToEndWhenExceptionHappened() {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new Exception("some error"));
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(USER_DOCUMENTS, TestDocument.class).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertNotNull(docs.getCurrentPage());
        assertNotNull(docs.getCurrentPage().getError());

        /* Make the call, when continuation token is null. */
        Page nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());

        /* Set the continuation token, but the http call failed. */
        docs.setContinuationToken("fake continuation token").setTokenResult(Utils.getGson().fromJson(tokenResult, TokenResult.class)).setHttpClient(mHttpClient);
        nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());
    }

    @Test
    public void listEndToEndWhenMakeTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<PaginatedDocuments<TestDocument>> documents = Data.list(USER_DOCUMENTS, TestDocument.class);

        String exceptionMessage = "Call to token exchange failed for whatever reason";
        verifyTokenExchangeFlow(null, new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(documents);
        assertNotNull(documents.get());
        assertNotNull(documents.get().getCurrentPage().getError());
        assertThat(
                documents.get().getCurrentPage().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void replaceEndToEnd() throws JSONException {

        /* Mock http call to get token. */
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.replace(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);

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
    public void readFailsToDeserializeDocumentDoesNotThrow() throws JSONException {

        /* Mock http call to get token. */
        /* Pass incorrect document type to cause serialization failure (document is of type TestDocument). */
        AppCenterFuture<DocumentWrapper<String>> doc = Data.read(DefaultPartitions.USER_DOCUMENTS, DOCUMENT_ID, String.class);

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
    public void createFailsToDeserializeDocumentDoesNotThrow() throws JSONException {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DefaultPartitions.USER_DOCUMENTS, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, new WriteOptions());

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
    public void listFailsToDeserializeDocumentDoesNotThrow() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<DocumentWrapper<TestDocument>> documents = Collections.singletonList(new DocumentWrapper<>(
                new TestDocument("Test"),
                RESOLVED_USER_PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedResponse = Utils.getGson().toJson(
                new Page<TestDocument>().setItems(documents)
        );

        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. Ensure deserialization error on document by passing incorrect class type. */
        AppCenterFuture<PaginatedDocuments<String>> result = Data.list(DefaultPartitions.USER_DOCUMENTS, String.class);

        verifyNoMoreInteractions(mLocalDocumentStorage);

        /* Verify the result is correct. */
        assertNotNull(result);
        PaginatedDocuments<String> docs = result.get();
        assertNotNull(docs);
        assertFalse(docs.hasNextPage());
        Page<String> page = docs.getCurrentPage();
        assertNotNull(page);
        assertNull(page.getError());
        assertNotNull(page.getItems());
        assertEquals(1, page.getItems().size());
        assertTrue(page.getItems().get(0).hasFailed());
    }

    @Test
    public void listFailsToDeserializeListOfDocumentsDoesNotThrow() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Setup list documents api response. Set response as empty string to force deserialization error. */
        final String expectedResponse = "";
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        AppCenterFuture<PaginatedDocuments<TestDocument>> result = Data.list(DefaultPartitions.USER_DOCUMENTS, TestDocument.class);

        /* Verify the result is correct and the cache was not touched. */
        verifyNoMoreInteractions(mLocalDocumentStorage);
        assertNotNull(result);
        PaginatedDocuments<TestDocument> docs = result.get();
        assertNotNull(docs);
        assertFalse(docs.hasNextPage());
        Page<TestDocument> page = docs.getCurrentPage();
        assertNotNull(page);
        assertNotNull(page.getError());
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
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(partition, DOCUMENT_ID, TestDocument.class);

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
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(USER_DOCUMENTS, DOCUMENT_ID, TestDocument.class);
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
        String documentID = "Test Document";
        Data.read(USER_DOCUMENTS, documentID, TestDocument.class);
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
    public void readFailTokenExchangeReturnsFailedTokenResultPayload() {
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(USER_DOCUMENTS, DOCUMENT_ID, TestDocument.class);

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
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.read(USER_DOCUMENTS, DOCUMENT_ID, TestDocument.class);

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
        Data.read(USER_DOCUMENTS, DOCUMENT_ID, TestDocument.class);
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
        DocumentWrapper<String> document = Data.read(USER_DOCUMENTS, DOCUMENT_ID, String.class).get();
        assertNotNull(document.getError());
        assertTrue(document.getError().getMessage().contains(failedMessage));
    }

    @Test
    public void readOnLineWhenLocalStorageContainsNullOperation() {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        TokenResult tokenResult = new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("tokenResult");
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
        DocumentWrapper<String> document = Data.read(USER_DOCUMENTS, DOCUMENT_ID, String.class).get();
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
        DocumentWrapper<String> document = Data.read(USER_DOCUMENTS, DOCUMENT_ID, String.class).get();
        verifyNoMoreInteractions(mHttpClient);
        assertEquals(createdDocument.getDeserializedValue(), document.getDeserializedValue());
    }

    @Test
    public void createEndToEndWithNetwork() throws JSONException {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        WriteOptions writeOptions = new WriteOptions(12476);
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, writeOptions);
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
        final PendingOperation pendingOperation = new PendingOperation(
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
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify additional headers does not contain the upsert key. */
        assertNull(headers.getValue());
    }

    @Test
    public void replacePendingOperationWithUpsertHeader() {

        /* If we have one pending operation delete, and the network is on. */
        final PendingOperation pendingOperation = new PendingOperation(
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
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

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
        Data.create(USER_DOCUMENTS, "123", "test", String.class).get();

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
        Data.replace(USER_DOCUMENTS, "123", "test", String.class).get();

        /* Verify the additional header does not contain upsert key. */
        assertNotNull(headers.getValue());
    }

    @Test
    public void createWithNoNetwork() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        TestDocument testDocument = new TestDocument("test");
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        Data.create(USER_DOCUMENTS, DOCUMENT_ID, testDocument, TestDocument.class);
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
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument("test"), TestDocument.class);
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
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument("test"), TestDocument.class);
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
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mLocalDocumentStorage).deleteOnline(eq(USER_TABLE_NAME), eq(RESOLVED_USER_PARTITION), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        assertNotNull(doc.get());
        assertFalse(doc.get().hasFailed());
        assertNull(doc.get().getError());
    }

    @Test
    public void deleteTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
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
        AppCenterFuture<DocumentWrapper<Void>> doc = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
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
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(cachedDocument);
        when(mLocalDocumentStorage.deleteOffline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID))).thenReturn(true);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
        verify(mLocalDocumentStorage).deleteOffline(eq(USER_TABLE_NAME), eq(RESOLVED_USER_PARTITION), eq(DOCUMENT_ID));
        verify(mLocalDocumentStorage, never()).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertNull(result.get().getError());
    }

    @Test
    public void deleteWithoutNetworkFails() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        DocumentWrapper<Void> cachedDocument = new DocumentWrapper<>(null, DefaultPartitions.USER_DOCUMENTS, DOCUMENT_ID, "someETag", System.currentTimeMillis() + 6000);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(cachedDocument);
        when(mLocalDocumentStorage.deleteOffline(eq(USER_TABLE_NAME), anyString(), anyString())).thenReturn(false);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
        verify(mLocalDocumentStorage).deleteOffline(eq(USER_TABLE_NAME), eq(RESOLVED_USER_PARTITION), eq(DOCUMENT_ID));
        verify(mLocalDocumentStorage, never()).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(result.get().getError());
    }

    @Test
    public void deleteSuccessfullyFromLocalStorageWithoutNetworkCallWhenDocumentCreatedOnlyOffline() {
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(TOKEN_RESULT);
        when(mLocalDocumentStorage.read(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID), eq(Void.class), isNull(ReadOptions.class))).thenReturn(new DocumentWrapper<Void>());
        when(mLocalDocumentStorage.deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID))).thenReturn(true);
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
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
        AppCenterFuture<DocumentWrapper<Void>> result = Data.delete(USER_DOCUMENTS, DOCUMENT_ID);
        verify(mLocalDocumentStorage).deleteOnline(eq(USER_TABLE_NAME), eq(USER_DOCUMENTS + "-" + ACCOUNT_ID), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mHttpClient);
        assertTrue(result.get().hasFailed());
        assertNotNull(result.get().getError());
    }

    @Test
    public void buildAppCenterGetDbTokenBodyPayload() {
        String expectedPayload = "{\"partitions\":[\"test\"]}";
        String payload = TokenExchange.buildAppCenterGetDbTokenBodyPayload("test");
        assertEquals(expectedPayload, payload);
    }

    @Test
    public void documentDeserialization() {
        DocumentWrapper<TestDocument> d =
                Utils.parseDocument(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, TestDocument.class);
        assertEquals(DOCUMENT_ID, d.getId());
        assertEquals(RESOLVED_USER_PARTITION, d.getPartition());
        assertEquals(TEST_FIELD_VALUE, d.getDeserializedValue().test);
        assertEquals(ETAG, d.getETag());
        assertEquals(1550881731, d.getLastUpdatedDate());
    }

    @Test
    public void documentSerialization() {
        String jsonDocument = String.format("{\"test\": \"%s\"\n" + "}", TEST_FIELD_VALUE);
        TestDocument deserializedDocument = Utils.fromJson(jsonDocument, TestDocument.class);
        assertEquals(TEST_FIELD_VALUE, deserializedDocument.test);
    }

    @Test
    public void generateHeaders() {
        Map<String, String> headers = CosmosDb.addRequiredHeaders(new HashMap<String, String>(), RESOLVED_USER_PARTITION, "token");
        assertEquals(5, headers.size());
    }

    @Test
    public void generateAdditionalHeaders() {
        Map<String, String> headers =
                CosmosDb.addRequiredHeaders(
                        new HashMap<String, String>() {{
                            put("extra", "header");
                        }},
                        RESOLVED_USER_PARTITION,
                        "token");
        assertEquals(6, headers.size());
        String extra = headers.get("extra");
        assertNotNull(extra);
        assertTrue(headers.containsKey("extra") && extra.equals("header"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void urlEncodingThrowsNonExistingEncoding() {
        CosmosDb.urlEncode("a string to encode", "An encoding that doesn't exist");
    }

    @Test
    public void cosmosDbCallFailsShouldNotThrow() {

        /* Setup after get the token from cache, all the http call will fail. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult().setPartition(RESOLVED_USER_PARTITION).setExpirationDate(expirationDate.getTime()).setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + RESOLVED_USER_PARTITION)).thenReturn(tokenResult);
        when(mLocalDocumentStorage.read(anyString(), anyString(), anyString(), eq(TestDocument.class), any(ReadOptions.class))).thenReturn(new DocumentWrapper<TestDocument>(new Exception("read error.")));
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(403, "The operation is forbidden."));
                return mock(ServiceCall.class);
            }
        });

        /* Make the call and verify. */
        Data.read(RESOLVED_USER_PARTITION, DOCUMENT_ID, TestDocument.class);
        Data.delete(RESOLVED_USER_PARTITION, DOCUMENT_ID);
        Data.create(RESOLVED_USER_PARTITION, DOCUMENT_ID, new TestDocument("test"), TestDocument.class);
        Data.list(RESOLVED_USER_PARTITION, TestDocument.class);
    }

    @Test
    public void canCancelWhenCallNotFinished() {
        mockStatic(CosmosDb.class);
        mockStatic(TokenExchange.class);
        ServiceCall mockServiceCall = mock(ServiceCall.class);
        when(TokenExchange.getDbToken(anyString(), any(HttpClient.class), anyString(), anyString(), any(TokenExchange.TokenExchangeServiceCallback.class))).thenReturn(mockServiceCall);
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);
        Data.setEnabled(false).get();
        assertNull(doc.get());
        verify(mockServiceCall).cancel();
    }

    @Test
    public void shouldNotOperateOnLocalStorageWhenPartitionNotExist() {

        /* Setup the network is disconnected. */
        String failedMessage = "Unable to find partition";
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Make the call to create local document in local storage. */
        DocumentWrapper<TestDocument> doc = Data.create(USER_DOCUMENTS, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class).get();

        /* Local storage create document should complete with not find partition error. */
        assertNotNull(doc);
        assertNotNull(doc.getError());
        assertTrue(doc.getError().getMessage().contains(failedMessage));

        /* Make the call to read local document from local storage. */
        doc = Data.read(USER_DOCUMENTS, DOCUMENT_ID, TestDocument.class).get();

        /* Local storage read document should complete with not find partition error. */
        assertNotNull(doc);
        assertNotNull(doc.getError());
        assertTrue(doc.getError().getMessage().contains(failedMessage));

        /* Make the call to delete local document from local storage. */
        DocumentWrapper<Void> deleteDocument = Data.delete(USER_DOCUMENTS, DOCUMENT_ID).get();

        /* Local storage delete document should complete with not find partition error. */
        assertNotNull(deleteDocument);
        assertNotNull(deleteDocument.getError());
        assertTrue(deleteDocument.getError().getMessage().contains(failedMessage));
    }

    @Test
    public void pendingOperationProcessedWhenNetworkOnAndApplyAppEnabled() throws JSONException {

        /* If we have one pending operation delete, and the network is on. */
        final PendingOperation pendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                DOCUMENT_ID,
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Collections.singletonList(pendingOperation));
        ArgumentCaptor<DocumentMetadata> documentMetadataArgumentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);

        /* Set up listener. */
        Data.unsetInstance();
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify pending operation get processed. */
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(PENDING_OPERATION_DELETE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DataException.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mDataStoreEventListener);
        assertEquals(DOCUMENT_ID, documentMetadata.getId());
        assertEquals(RESOLVED_USER_PARTITION, documentMetadata.getPartition());
        assertNull(documentMetadata.getETag());
        verify(mLocalDocumentStorage).deleteOnline(eq(pendingOperation.getTable()), eq(pendingOperation.getPartition()), eq(pendingOperation.getDocumentId()));
    }

    @Test
    public void pendingOperationNotProcessedWhenNetworkOff() {

        /* If we have one pending operation delete, and the network is off. */
        final PendingOperation pendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                DOCUMENT_ID,
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Collections.singletonList(pendingOperation));

        /* Set up listener. */
        Data.unsetInstance();
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify pending operation is not get processed. */
        verify(mDataStoreEventListener, never()).onDataStoreOperationResult(
                anyString(),
                any(DocumentMetadata.class),
                any(DataException.class));
        verifyNoMoreInteractions(mDataStoreEventListener);
        verify(mLocalDocumentStorage, never()).updatePendingOperation(eq(pendingOperation));
    }

    @Test
    public void pendingOperationNotProcessedWhenApplyEnabledFalse() {

        /* If we have delete, create, update pending operation, and the network is on. */
        final PendingOperation deletePendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything1",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        final PendingOperation createPendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_CREATE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything2",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        final PendingOperation replacePendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything3",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME))
                .thenReturn(Arrays.asList(deletePendingOperation, createPendingOperation, replacePendingOperation));

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
        ServiceCall serviceCallMock1 = mock(ServiceCall.class);
        ServiceCall serviceCallMock2 = mock(ServiceCall.class);
        ServiceCall serviceCallMock3 = mock(ServiceCall.class);
        when(mHttpClient.callAsync(anyString(), anyString(),
                anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class)))
                .thenReturn(serviceCallMock1).thenReturn(serviceCallMock2).thenReturn(serviceCallMock3);

        /* Set up listener. */
        Data.unsetInstance();
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Await the result to make sure that disabling has completed by the time we verify. */
        Data.setEnabled(false).get();

        /* Verify the service call has been canceled. */
        verify(mDataStoreEventListener, never()).onDataStoreOperationResult(
                anyString(),
                any(DocumentMetadata.class),
                any(DataException.class));
        verifyNoMoreInteractions(mDataStoreEventListener);
        verify(mLocalDocumentStorage, never()).updatePendingOperation(eq(deletePendingOperation));
        verify(serviceCallMock1).cancel();
        verify(serviceCallMock2).cancel();
        verify(serviceCallMock3).cancel();
    }

    @Test
    public void pendingOperationProcessedOnceWhenDuplicatePendingOperations() throws JSONException {

        /* If we have duplicate pending operation. (Mock the situation we have call processPendingOperation at the same time.) */
        final PendingOperation pendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                DOCUMENT_ID,
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Arrays.asList(pendingOperation, pendingOperation));
        ArgumentCaptor<DocumentMetadata> documentMetadataArgumentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);

        /* Set up listener. */
        Data.unsetInstance();
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify only one pending operation has been executed. */
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(PENDING_OPERATION_DELETE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DataException.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mDataStoreEventListener);
        assertEquals(DOCUMENT_ID, documentMetadata.getId());
        assertEquals(RESOLVED_USER_PARTITION, documentMetadata.getPartition());
        assertNull(documentMetadata.getETag());
        verify(mLocalDocumentStorage).deleteOnline(eq(pendingOperation.getTable()), eq(pendingOperation.getPartition()), eq(pendingOperation.getDocumentId()));
    }

    @Test
    public void partiallySavedPendingOperationDoesNotThrowExceptionWhenDisabled() {

        /* If we have one pending operation, and network is on. */
        final PendingOperation deletePendingOperation = new PendingOperation(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything1",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME)).thenReturn(Collections.singletonList(deletePendingOperation));

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

        /* Return null service call to simulate a partially saved pending operation. */
        when(mHttpClient.callAsync(anyString(), anyString(),
                anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class)))
                .thenReturn(null);

        /* Set up listener. */
        Data.unsetInstance();
        Data.setDataStoreRemoteOperationListener(mDataStoreEventListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Ensure that this does not throw. */
        Data.setEnabled(false).get();
    }

    @Test
    public void corruptedTokenDoesNotCrash() {

        /* If we get invalid token from cache. */
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + APP_DOCUMENTS)).thenReturn("garbage");

        /* When we perform an operation. */
        AppCenterFuture<DocumentWrapper<TestDocument>> future = Data.read(DefaultPartitions.APP_DOCUMENTS, "docId", TestDocument.class);

        /* Then we'll refresh token online. */
        ArgumentCaptor<ServiceCallback> captor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), captor.capture());

        /* If we also get corrupted json online for token. */
        captor.getValue().onCallSucceeded("garbage", new HashMap<String, String>());

        /* Then the call fails. */
        future.get();
        assertNull(future.get().getDeserializedValue());
        assertNotNull(future.get().getError());
        assertTrue(future.get().getError().getCause() instanceof DataException);
        assertTrue(future.get().getError().getCause().getCause() instanceof JsonSyntaxException);
    }
}
