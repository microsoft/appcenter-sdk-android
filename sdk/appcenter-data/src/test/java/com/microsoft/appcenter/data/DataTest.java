/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.Context;

import com.google.gson.JsonSyntaxException;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentMetadata;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.TokenResult;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

public class DataTest extends AbstractDataTest {

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
        assertEquals(new Date(1550881731000L), d.getLastUpdatedDate());
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
        Data.read(DOCUMENT_ID, TestDocument.class, RESOLVED_USER_PARTITION);
        Data.delete(DOCUMENT_ID, RESOLVED_USER_PARTITION);
        Data.create(DOCUMENT_ID, new TestDocument("test"), TestDocument.class, RESOLVED_USER_PARTITION);
        Data.list(TestDocument.class, RESOLVED_USER_PARTITION);
    }

    @Test
    public void canCancelWhenCallNotFinished() {
        mockStatic(CosmosDb.class);
        mockStatic(TokenExchange.class);
        ServiceCall mockServiceCall = mock(ServiceCall.class);
        when(TokenExchange.getDbToken(anyString(), any(HttpClient.class), anyString(), anyString(), any(TokenExchange.TokenExchangeServiceCallback.class))).thenReturn(mockServiceCall);
        AppCenterFuture<DocumentWrapper<TestDocument>> doc = Data.create(DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS);
        Data.setEnabled(false).get();
        assertNull(doc.get());
        verify(mockServiceCall).cancel();
    }

    @Test
    public void failFastForInvalidDocumentId() {
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                fail("Http Client should not have been accessed.");
                return null;
            }
        });


        /* Document IDs cannot be null or empty, or contain '#', '/', or '\'. */
        ArrayList<String> invalidDocumentIds = new ArrayList<String>() {
            {
                add(null);
                add("");
                add("#");
                add("abc#");
                add("#abc");
                add("ab#c");
                add("/");
                add("abc/");
                add("/abc");
                add("ab/c");
                add("\\");
                add("abc\\");
                add("\\abc");
                add("ab\\c");
                add(" ");
                add("abc ");
                add(" abc");
                add("ab c");
                add("?");
                add("abc?");
                add("?abc");
                add("ab?c");
                add("\t");
                add("abc\t");
                add("\tabc");
                add("ab\tc");
                add("\n");
                add("abc\n");
                add("\nabc");
                add("ab\nc");
            }
        };
        for (String invalidId : invalidDocumentIds) {

            /* Execute each operation that uses a document ID. */
            DocumentWrapper<TestDocument> createDoc = Data.create(invalidId, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS).get();
            DocumentWrapper<TestDocument> readDoc = Data.read(invalidId, TestDocument.class, USER_DOCUMENTS).get();
            DocumentWrapper<TestDocument> replaceDoc = Data.replace(invalidId, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS).get();

            /* All results must have errors. */
            assertNotNull(createDoc.getError());
            assertNotNull(readDoc.getError());
            assertNotNull(replaceDoc.getError());
        }

        /* Ensure that local document storage has not been accessed. */
        verifyNoMoreInteractions(mLocalDocumentStorage);
    }

    @Test
    public void shouldNotOperateOnLocalStorageWhenPartitionNotExist() {

        /* Setup the network is disconnected. */
        String failedMessage = "Unable to find partition";
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Make the call to create local document in local storage. */
        DocumentWrapper<TestDocument> doc = Data.create(DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class, USER_DOCUMENTS).get();

        /* Local storage create document should complete with not find partition error. */
        assertNotNull(doc);
        assertNotNull(doc.getError());
        assertTrue(doc.getError().getMessage().contains(failedMessage));

        /* Make the call to read local document from local storage. */
        doc = Data.read(DOCUMENT_ID, TestDocument.class, USER_DOCUMENTS).get();

        /* Local storage read document should complete with not find partition error. */
        assertNotNull(doc);
        assertNotNull(doc.getError());
        assertTrue(doc.getError().getMessage().contains(failedMessage));

        /* Make the call to delete local document from local storage. */
        DocumentWrapper<Void> deleteDocument = Data.delete(DOCUMENT_ID, USER_DOCUMENTS).get();

        /* Local storage delete document should complete with not find partition error. */
        assertNotNull(deleteDocument);
        assertNotNull(deleteDocument.getError());
        assertTrue(deleteDocument.getError().getMessage().contains(failedMessage));
    }


    @Test
    public void pendingOperationProcessedWhenNetworkOnAndApplyAppEnabled() throws JSONException {

        /* If we have one pending operation delete, and the network is on. */
        final LocalDocument pendingOperation = new LocalDocument(
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
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify pending operation get processed. */
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mRemoteOperationListener).onRemoteOperationCompleted(
                eq(PENDING_OPERATION_DELETE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DataException.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mRemoteOperationListener);
        assertEquals(DOCUMENT_ID, documentMetadata.getId());
        assertEquals(RESOLVED_USER_PARTITION, documentMetadata.getPartition());
        assertNull(documentMetadata.getETag());
        verify(mLocalDocumentStorage).deleteOnline(eq(pendingOperation.getTable()), eq(pendingOperation.getPartition()), eq(pendingOperation.getDocumentId()));
    }

    @Test
    public void pendingOperationNotProcessedWhenNetworkOff() {

        /* If we have one pending operation delete, and the network is off. */
        final LocalDocument pendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
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
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify pending operation is not get processed. */
        verify(mRemoteOperationListener, never()).onRemoteOperationCompleted(
                anyString(),
                any(DocumentMetadata.class),
                any(DataException.class));
        verifyNoMoreInteractions(mRemoteOperationListener);
        verify(mLocalDocumentStorage, never()).updatePendingOperation(eq(pendingOperation));
    }

    @Test
    public void pendingOperationNotProcessedWhenApplyEnabledFalse() {

        /* If we have delete, create, update pending operation, and the network is on. */
        final LocalDocument deletePendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything1",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        final LocalDocument createPendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_CREATE_VALUE,
                RESOLVED_USER_PARTITION,
                "anything2",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        final LocalDocument replacePendingOperation = new LocalDocument(
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
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Await the result to make sure that disabling has completed by the time we verify. */
        Data.setEnabled(false).get();

        /* Verify the service call has been canceled. */
        verify(mRemoteOperationListener, never()).onRemoteOperationCompleted(
                anyString(),
                any(DocumentMetadata.class),
                any(DataException.class));
        verifyNoMoreInteractions(mRemoteOperationListener);
        verify(mLocalDocumentStorage, never()).updatePendingOperation(eq(deletePendingOperation));
        verify(serviceCallMock1).cancel();
        verify(serviceCallMock2).cancel();
        verify(serviceCallMock3).cancel();
    }

    @Test
    public void pendingOperationProcessedOnceWhenDuplicatePendingOperations() throws JSONException {

        /* If we have duplicate pending operation. (Mock the situation we have call processPendingOperation at the same time.) */
        final LocalDocument pendingOperation = new LocalDocument(
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
        Data.setRemoteOperationListener(mRemoteOperationListener);

        /* Start data. */
        mData = Data.getInstance();
        mChannel = start(mData);

        /* Verify only one pending operation has been executed. */
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, TOKEN_EXCHANGE_USER_PAYLOAD, METHOD_DELETE, "", null);
        verify(mRemoteOperationListener).onRemoteOperationCompleted(
                eq(PENDING_OPERATION_DELETE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DataException.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mRemoteOperationListener);
        assertEquals(DOCUMENT_ID, documentMetadata.getId());
        assertEquals(RESOLVED_USER_PARTITION, documentMetadata.getPartition());
        assertNull(documentMetadata.getETag());
        verify(mLocalDocumentStorage).deleteOnline(eq(pendingOperation.getTable()), eq(pendingOperation.getPartition()), eq(pendingOperation.getDocumentId()));
    }

    @Test
    public void partiallySavedPendingOperationDoesNotThrowExceptionWhenDisabled() {

        /* If we have one pending operation, and network is on. */
        final LocalDocument deletePendingOperation = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
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
        Data.setRemoteOperationListener(mRemoteOperationListener);

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
        AppCenterFuture<DocumentWrapper<TestDocument>> future = Data.read("docId", TestDocument.class, DefaultPartitions.APP_DOCUMENTS);

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

    @Test
    public void httpClientCreatedWithoutCompression() {
        mockStatic(HttpUtils.class);

        /* When Data class is initialized. */
        Data.unsetInstance();
        start(Data.getInstance());

        /* Http Client must be created without compression. */
        verifyStatic();
        HttpUtils.createHttpClient(any(Context.class), eq(false));
    }

    @Test
    public void moduleHasNotStartedDoesNotThrow() {
        Data.unsetInstance();
        verifyIllegalStateResult();
    }

    @Test
    public void moduleStartedButDisabled() {
        Data.setEnabled(false);
        verifyIllegalStateResult();
    }

    private void verifyIllegalStateResult() {

        /* Test `create` before module started */
        DocumentWrapper<TestDocument> createDoc = Data.create("id", new TestDocument("a"), TestDocument.class, DefaultPartitions.APP_DOCUMENTS).get();
        assertNull(createDoc.getDeserializedValue());
        assertNotNull(createDoc.getError());
        assertEquals(IllegalStateException.class, createDoc.getError().getCause().getClass());

        /* Test `replace` before module started */
        DocumentWrapper<TestDocument> replaceDoc = Data.replace("id", new TestDocument("a"), TestDocument.class, DefaultPartitions.APP_DOCUMENTS).get();
        assertNull(replaceDoc.getDeserializedValue());
        assertNotNull(replaceDoc.getError());
        assertEquals(IllegalStateException.class, replaceDoc.getError().getCause().getClass());

        /* Test `read` before module started */
        DocumentWrapper<TestDocument> readDoc = Data.read("id", TestDocument.class, DefaultPartitions.APP_DOCUMENTS).get();
        assertNull(readDoc.getDeserializedValue());
        assertNotNull(readDoc.getError());
        assertEquals(IllegalStateException.class, readDoc.getError().getCause().getClass());

        /* Test `list` before module started */
        PaginatedDocuments<TestDocument> listDoc = Data.list(TestDocument.class, DefaultPartitions.USER_DOCUMENTS).get();
        assertNull(listDoc.getCurrentPage().getItems());
        assertNotNull(listDoc.getCurrentPage().getError());
        assertEquals(IllegalStateException.class, listDoc.getCurrentPage().getError().getCause().getClass());

        /* Test `delete` before module started */
        DocumentWrapper<Void> deleteDoc = Data.delete("id", DefaultPartitions.USER_DOCUMENTS).get();
        assertNull(deleteDoc.getDeserializedValue());
        assertNotNull(deleteDoc.getError());
        assertEquals(IllegalStateException.class, deleteDoc.getError().getCause().getClass());
    }
}
