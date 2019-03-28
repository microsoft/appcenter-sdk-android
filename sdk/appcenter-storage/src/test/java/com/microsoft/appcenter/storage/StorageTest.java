/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.accounts.NetworkErrorException;

import com.google.gson.Gson;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.StorageHttpClientDecorator;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.storage.Constants.PARTITION_NAMES;
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
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.matches;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({
        CosmosDb.class,
        TokenExchange.class,
        TokenManager.class
})
public class StorageTest extends AbstractStorageTest {

    private static final String DATABASE_NAME = "mbaas";
    private static final String COLLECTION_NAME = "appcenter";
    private static final String PARTITION = "custom-partition";
    private static final String DOCUMENT_ID = "document-id";
    private static final String TEST_FIELD_VALUE = "Test Value";
    private static String tokenExchangeResponsePayload = String.format("{\n" +
            "    \"tokens\": [\n" +
            "        {\n" +
            "            \"partition\": \"%s\",\n" +
            "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
            "            \"dbName\": \"%s\",\n" +
            "            \"dbCollectionName\": \"%s\",\n" +
            "            \"token\": \"ha-ha-ha-ha\",\n" +
            "            \"status\": \"Succeed\"\n" +
            "        }\n" +
            "    ]\n" +
            "}", PARTITION, DATABASE_NAME, COLLECTION_NAME);

    private static String COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD = String.format("{\n" +
            "    \"document\": {\n" +
            "        \"test\": \"%s\"\n" +
            "    },\n" +
            "    \"id\": \"%s\",\n" +
            "    \"PartitionKey\": \"%s\",\n" +
            "    \"_rid\": \"mFBtAPPa528HAAAAAAAAAA==\",\n" +
            "    \"_self\": \"dbs/mFBtAA==/colls/mFBtAPPa528=/docs/mFBtAPPa528HAAAAAAAAAA==/\",\n" +
            "    \"_etag\": \"06000da6-0000-0000-0000-5c7093c30000\",\n" +
            "    \"_attachments\": \"attachments/\",\n" +
            "    \"_ts\": 1550881731\n" +
            "}", TEST_FIELD_VALUE, DOCUMENT_ID, PARTITION);

    @Test
    public void singleton() {
        Assert.assertSame(Storage.getInstance(), Storage.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Storage.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Storage.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {
        Storage.setEnabled(true);

        verify(mChannel).removeGroup(eq(mStorage.getGroupName()));
        verify(mChannel).addGroup(eq(mStorage.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Storage.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Storage.setEnabled(false).get();
        assertFalse(Storage.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(STORAGE_ENABLED_KEY, true)).thenReturn(false);
        verify(mChannel, never()).removeListener(any(Channel.Listener.class));
        verify(mChannel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void listEndToEndWhenSinglePage() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(PARTITION).withExpirationTime(expirationDate.getTime()).withToken("fakeToken"));
        when(SharedPreferencesManager.getString(PARTITION)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<Document<TestDocument>> documents = Collections.singletonList(new Document<>(
                new TestDocument("Test"),
                PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedResponse = new Gson().toJson(
                new Page<TestDocument>().withDocuments(documents)
        );
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Storage.list(PARTITION, TestDocument.class).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertEquals(1, docs.getCurrentPage().getItems().size());
        assertEquals(docs.getCurrentPage().getItems().get(0).getDocument().test, documents.get(0).getDocument().test);
    }

    @Test
    public void listEndToEndWhenMultiplePages() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(PARTITION).withExpirationTime(expirationDate.getTime()).withToken("fakeToken"));
        when(SharedPreferencesManager.getString(PARTITION)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<Document<TestDocument>> firstPartDocuments = Collections.singletonList(new Document<>(
                new TestDocument("Test"),
                PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedFirstResponse = new Gson().toJson(
                new Page<TestDocument>().withDocuments(firstPartDocuments)
        );
        final List<Document<TestDocument>> secondPartDocuments = Collections.singletonList(new Document<>(
                new TestDocument("Test2"),
                PARTITION,
                "document id 2",
                "e tag 2",
                1
        ));
        final String expectedSecondResponse = new Gson().toJson(
                new Page<TestDocument>().withDocuments(secondPartDocuments)
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
        PaginatedDocuments<TestDocument> docs = Storage.list(PARTITION, TestDocument.class).get();
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
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(PARTITION).withExpirationTime(expirationDate.getTime()).withToken("fakeToken"));
        when(SharedPreferencesManager.getString(PARTITION)).thenReturn(tokenResult);

        /* Setup list documents api response. */
        List<Document<TestDocument>> firstPartDocuments = Collections.nCopies(2, new Document<>(
                new TestDocument("Test"),
                PARTITION,
                "document id",
                "e tag",
                0
        ));
        final String expectedFirstResponse = new Gson().toJson(
                new Page<TestDocument>().withDocuments(firstPartDocuments)
        );
        final List<Document<TestDocument>> secondPartDocuments = Collections.singletonList(new Document<>(
                new TestDocument("Test2"),
                PARTITION,
                "document id 2",
                "e tag 2",
                1
        ));
        final String expectedSecondResponse = new Gson().toJson(
                new Page<TestDocument>().withDocuments(secondPartDocuments)
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
        Iterator<Document<TestDocument>> iterator = Storage.list(PARTITION, TestDocument.class).get().iterator();
        List<Document<TestDocument>> documents = new ArrayList<>();
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
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(PARTITION).withExpirationTime(expirationDate.getTime()).withToken("fakeToken"));
        when(SharedPreferencesManager.getString(PARTITION)).thenReturn(tokenResult);
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new Exception("some error"));
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Storage.list(PARTITION, TestDocument.class).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertNotNull(docs.getCurrentPage());
        assertNotNull(docs.getCurrentPage().getError());

        /* Make the call, when continuation token is null. */
        Page nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());

        /* Set the continuation token, but the http call failed. */
        docs.withContinuationToken("fake continuation token").withTokenResult(new Gson().fromJson(tokenResult, TokenResult.class)).withHttpClient(mHttpClient);
        nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());
    }

    @Test
    public void listEndToEndWhenMakeTokenExchangeCallFails() {
        AppCenterFuture<PaginatedDocuments<TestDocument>> documents = Storage.list(PARTITION, TestDocument.class);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                anyString(),
                anyString(),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        tokenExchangeServiceCallback.onCallFailed(new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(documents);
        assertNotNull(documents.get());
        assertNotNull(documents.get().getCurrentPage().getError());
        assertThat(
                documents.get().getCurrentPage().getError().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void replaceEndToEnd() {

        /* Mock http call to get token. */
        AppCenterFuture<Document<TestDocument>> doc = Storage.replace(PARTITION, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);

        /* Make the call. */
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);

        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, null)),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                cosmosDbServiceCallbackArgumentCaptor.capture());

        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, new HashMap<String, String>());

        /* Get and verify token. */
        assertNotNull(doc);

        Document<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertEquals(PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getEtag());
        assertNotEquals(0L, testCosmosDocument.getTimestamp());

        TestDocument testDocument = testCosmosDocument.getDocument();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void readEndToEnd() throws Exception {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

        ArgumentCaptor<HttpClient.CallTemplate> tokenExchangeCallTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verifyNoMoreInteractions(mLocalDocumentStorage);
        verify(mHttpClient, times(1)).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                tokenExchangeCallTemplateCallbackArgumentCaptor.capture(),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        tokenExchangeCallTemplateCallbackArgumentCaptor.getValue().buildRequestBody();
        tokenExchangeCallTemplateCallbackArgumentCaptor.getValue().onBeforeCalling(null, new HashMap<String, String>());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<HttpClient.CallTemplate> cosmosDbCallTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient, times(1)).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, DOCUMENT_ID)),
                eq(METHOD_GET),
                anyMapOf(String.class, String.class),
                cosmosDbCallTemplateCallbackArgumentCaptor.capture(),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        cosmosDbCallTemplateCallbackArgumentCaptor.getValue().buildRequestBody();
        cosmosDbCallTemplateCallbackArgumentCaptor.getValue().onBeforeCalling(null, new HashMap<String, String>());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, new HashMap<String, String>());

        assertNotNull(doc);

        Document<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertEquals(PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getEtag());
        assertNotEquals(0L, testCosmosDocument.getTimestamp());

        TestDocument testDocument = testCosmosDocument.getDocument();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);

        /* The test passes individually, but not in a suit. Need to figure out which mocks do not get reset. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);
        verifyNoMoreInteractions(mHttpClient);
        verify(mLocalDocumentStorage).read(
                eq(PARTITION),
                eq(DOCUMENT_ID),
                eq(testDocument.getClass()),
                any(ReadOptions.class));
    }

    @Test
    public void readFailTokenExchangeReturnsFailedTokenResultPayload() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

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
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(tokenExchangeFailedResponsePayload));
    }

    @Test
    public void readTokenExchangeCallFails() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

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
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        tokenExchangeServiceCallback.onCallFailed(new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void createEndToEnd() {
        WriteOptions writeOptions = new WriteOptions(12476);
        AppCenterFuture<Document<TestDocument>> doc = Storage.create(PARTITION, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, writeOptions);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, null)),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, new HashMap<String, String>());

        assertNotNull(doc);

        Document<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        verify(mLocalDocumentStorage, times(1)).write(refEq(testCosmosDocument), refEq(writeOptions));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        assertEquals(PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getEtag());
        assertNotEquals(0L, testCosmosDocument.getTimestamp());

        TestDocument testDocument = testCosmosDocument.getDocument();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void createTokenExchangeCallFails() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.create(PARTITION, DOCUMENT_ID, new TestDocument("test"), TestDocument.class);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                anyString(),
                anyString(),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        tokenExchangeServiceCallback.onCallFailed(new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void deleteEndToEnd() {
        AppCenterFuture<Document<Void>> doc = Storage.delete(PARTITION, DOCUMENT_ID);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());
        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, DOCUMENT_ID)),
                eq(METHOD_DELETE),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(null, new HashMap<String, String>());
        verify(mLocalDocumentStorage, times(1)).delete(eq(PARTITION), eq(DOCUMENT_ID));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNull(doc.get().getError());
    }

    @Test
    public void deleteTokenExchangeCallFails() {
        AppCenterFuture<Document<Void>> doc = Storage.delete(PARTITION, DOCUMENT_ID);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(mHttpClient).callAsync(
                anyString(),
                anyString(),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        tokenExchangeServiceCallback.onCallFailed(new HttpException(503, exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called.
         */
        verifyNoMoreInteractions(mHttpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void tokenClearedOnSignOut() {
        Set<String> partitionNames = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            partitionNames.add("partitionName " + i);
        }
        partitionNames.add(Constants.READONLY);
        when(SharedPreferencesManager.getStringSet(eq(PARTITION_NAMES))).thenReturn(partitionNames);
        Storage.setEnabled(true);
        AuthTokenContext.getInstance().setAuthToken(null, null, null);
        verifyStatic(times((10)));
        SharedPreferencesManager.remove(matches("partitionName [0-9]"));
    }

    @Test
    public void authTokenListenerNotCalledWhenDisabled() {
        Storage.setEnabled(false);
        AuthTokenContext.getInstance().setAuthToken(null, null, null);
        verifyStatic(never());
        SharedPreferencesManager.remove(matches("partitionName[0-9]"));
    }

    @Test
    public void authTokenListenerNotCalledWhenNewUser() {
        AuthTokenContext.getInstance().setAuthToken("someToken", "someId", new Date(Long.MAX_VALUE));
        AuthTokenContext.getInstance().setAuthToken(null, null, null);
        verifyStatic(never());
        SharedPreferencesManager.remove(matches("partitionName[0-9]"));
    }

    @Test
    public void authTokenListenerNotRemoveTokenWhenNewUser() {

        /* Setup token manager. */
        mockStatic(TokenManager.class);
        TokenManager mTokenManager = mock(TokenManager.class);
        when(TokenManager.getInstance()).thenReturn(mTokenManager);

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = mock(AuthTokenContext.Listener.class);

        /* Set new auth token. */
        AuthTokenContext.getInstance().addListener(mockListener);
        AuthTokenContext.getInstance().setAuthToken("mock-token", "mock-user", new Date(Long.MAX_VALUE));

        /* Verify. */
        verify(mTokenManager, times(0)).removeAllCachedTokens();
    }

    @Test
    public void buildAppCenterGetDbTokenBodyPayload() {
        String expectedPayload = "{\"partitions\":[\"test\"]}";
        String payload = TokenExchange.buildAppCenterGetDbTokenBodyPayload("test");
        assertEquals(expectedPayload, payload);
    }

    @Test
    public void documentDeserialization() {
        Document<TestDocument> d =
                Utils.parseDocument(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, TestDocument.class);
        assertEquals(DOCUMENT_ID, d.getId());
        assertEquals(PARTITION, d.getPartition());
        assertEquals(TEST_FIELD_VALUE, d.getDocument().test);
        assertEquals("06000da6-0000-0000-0000-5c7093c30000", d.getEtag());
        assertEquals(1550881731, d.getTimestamp());
    }

    @Test
    public void documentSerialization() {
        String jsonDocument = String.format("{\"test\": \"%s\"\n" + "}", TEST_FIELD_VALUE);
        TestDocument deserializedDocument = Utils.fromJson(jsonDocument, TestDocument.class);
        assertEquals(TEST_FIELD_VALUE, deserializedDocument.test);
    }

    @Test
    public void generateHeaders() {
        Map<String, String> headers = CosmosDb.addRequiredHeaders(new HashMap<String, String>(), PARTITION, "token");
        assertEquals(5, headers.size());
    }

    @Test
    public void generateAdditionalHeaders() {
        Map<String, String> headers =
                CosmosDb.addRequiredHeaders(
                        new HashMap<String, String>() {{
                            put("extra", "header");
                        }},
                        PARTITION,
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
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(PARTITION).withExpirationTime(expirationDate.getTime()).withToken("fakeToken"));
        when(SharedPreferencesManager.getString(PARTITION)).thenReturn(tokenResult);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(403, "The operation is forbidden."));
                return mock(ServiceCall.class);
            }
        });

        /* Make the call and verify. */
        Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);
        Storage.delete(PARTITION, DOCUMENT_ID);
        Storage.create(PARTITION, DOCUMENT_ID, new TestDocument("test"), TestDocument.class);
        Storage.list(PARTITION, TestDocument.class);
    }

    @Test
    public void canCancelWhenCallNotFinished() {
        mockStatic(CosmosDb.class);
        mockStatic(TokenExchange.class);
        ServiceCall mockServiceCall = mock(ServiceCall.class);
        when(TokenExchange.getDbToken(anyString(), any(HttpClient.class), anyString(), anyString(), any(TokenExchange.TokenExchangeServiceCallback.class))).thenReturn(mockServiceCall);
        AppCenterFuture<Document<TestDocument>> doc = Storage.create(PARTITION, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);
        Storage.setEnabled(false).get();
        assertNull(doc.get());
        verify(mockServiceCall).cancel();
    }

    @Test
    public void setStorageModuleOfflineMode() {
        assertFalse(Storage.isOfflineMode());
        Storage.setOfflineMode(true);

        /* offline mode is enabled. */
        assertTrue(Storage.isOfflineMode());

        /* offline mode is reset.  */
        Storage.setOfflineMode(false);
        assertFalse(Storage.isOfflineMode());
    }

    @Test
    public void offlineModeEnabledOnDecorator() {
        StorageHttpClientDecorator httpClientDecorator = new StorageHttpClientDecorator(mHttpClient);
        httpClientDecorator.setOfflineMode(true);
        ServiceCallback serviceCallback = Mockito.mock(ServiceCallback.class);
        httpClientDecorator.callAsync(null, null, null, null, serviceCallback);
        verify(serviceCallback).onCallFailed(isA(NetworkErrorException.class));
        verifyNoMoreInteractions(mHttpClient);
    }

    @Test
    public void offlineModeDisabledOnDecorator() {
        StorageHttpClientDecorator httpClientDecorator = new StorageHttpClientDecorator(mHttpClient);
        httpClientDecorator.setOfflineMode(false);
        String url = "url";
        String method = "method";
        Map<String, String> headers = new HashMap<>();
        httpClientDecorator.callAsync(url, method, headers, null, null);
        verify(mHttpClient).callAsync(eq(url), eq(method), eq(headers), isNull(HttpClient.CallTemplate.class), isNull(ServiceCallback.class));
    }

    @Test
    public void setOfflineModeBeforeStartDoesNotWork() {
        Storage.unsetInstance();
        Storage.setOfflineMode(true);
        assertFalse(Storage.isOfflineMode());
    }
}
