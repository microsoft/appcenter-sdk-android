package com.microsoft.appcenter.data;

import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;
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
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

public class DataListTest extends AbstractDataTest {

    @Captor
    private ArgumentCaptor<DocumentWrapper<TestDocument>> mTestDocumentWrapperCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeaders;

    private void mockSignOut() {
        Mockito.when(mAuthTokenContext.getAccountId()).thenReturn(null);
    }

    @Before
    public void setUpAuth() {
        setUpAuthContext();
    }

    @Test
    public void listAnObjectWhenThereArePendingOperations() {
        listAnObjectWhenThereArePendingOperations(Utils.getGson().toJson(new TestDocument("test")), TestDocument.class);
    }

    @Test
    public void listPrimitiveTypeWhenThereArePendingOperations() {
        listAnObjectWhenThereArePendingOperations("document", String.class);
    }

    private <T> void listAnObjectWhenThereArePendingOperations(String document, Class<T> documentType) {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Return list of one item which will have a non-expired pending operation. */
        final LocalDocument localDocument = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_CREATE_VALUE,
                RESOLVED_USER_PARTITION,
                "localDocument",
                document,
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP),
                expiredDocument = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
                RESOLVED_USER_PARTITION,
                "expiredDocument",
                document,
                PAST_TIMESTAMP,
                CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP),
                notPendingDocument = new LocalDocument(
                USER_TABLE_NAME,
                null,
                RESOLVED_USER_PARTITION,
                "notPendingDocument",
                document,
                PAST_TIMESTAMP,
                CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP),
                notPendingNotExpiredDocument = new LocalDocument(
                USER_TABLE_NAME,
                null,
                RESOLVED_USER_PARTITION,
                "notPendingNotExpiredDocument",
                document,
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        List<LocalDocument> storedDocuments = new ArrayList<LocalDocument>() {{
            add(localDocument);
            add(expiredDocument);
            add(notPendingDocument);
            add(notPendingNotExpiredDocument);
        }};
        assertTrue(LocalDocumentStorage.hasPendingOperation(storedDocuments));
        assertTrue(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(localDocument)));
        assertTrue(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(expiredDocument)));
        assertFalse(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(notPendingDocument)));
        assertFalse(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(notPendingNotExpiredDocument)));
        ReadOptions readOptions = new ReadOptions();
        when(mLocalDocumentStorage.getDocumentsByPartition(USER_TABLE_NAME, RESOLVED_USER_PARTITION, readOptions)).thenReturn(storedDocuments);
        PaginatedDocuments<T> documents = Data.list(documentType, USER_DOCUMENTS, readOptions).get();
        assertNull(documents.getCurrentPage().getError());
        List<DocumentWrapper<T>> items = documents.getCurrentPage().getItems();
        assertEquals(2, items.size());
        assertNull(items.get(0).getError());
        assertEquals(localDocument.getDocumentId(), items.get(0).getId());
        assertNull(items.get(1).getError());
        assertEquals(notPendingNotExpiredDocument.getDocumentId(), items.get(1).getId());
    }

    @Test
    public void expiredDocumentIsRemovedLocally() {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        LocalDocument localDocument = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_REPLACE_VALUE,
                RESOLVED_USER_PARTITION,
                "localDocument",
                "document",
                FUTURE_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP);
        LocalDocument expiredDocument = new LocalDocument(
                USER_TABLE_NAME,
                PENDING_OPERATION_DELETE_VALUE,
                RESOLVED_USER_PARTITION,
                "expiredDocument",
                "document",
                PAST_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
        );
        List<LocalDocument> storedDocuments = new ArrayList<>();
        storedDocuments.add(localDocument);
        storedDocuments.add(expiredDocument);
        assertTrue(LocalDocumentStorage.hasPendingOperation(storedDocuments));
        assertTrue(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(localDocument)));
        assertFalse(LocalDocumentStorage.hasPendingOperation(Collections.singletonList(expiredDocument)));
        ReadOptions readOptions = new ReadOptions();
        when(mLocalDocumentStorage.getDocumentsByPartition(USER_TABLE_NAME, RESOLVED_USER_PARTITION, readOptions)).thenReturn(storedDocuments);
        PaginatedDocuments<String> documents = Data.list(String.class, USER_DOCUMENTS, readOptions).get();
        List<DocumentWrapper<String>> items = documents.getCurrentPage().getItems();
        assertEquals(1, items.size());
    }


    @Test
    public void readOnlyListReturnsEmptyResult() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setAccountId(AbstractDataTest.ACCOUNT_ID)
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(new Date())
                .setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + APP_DOCUMENTS)).thenReturn(tokenResult);
        ReadOptions readOptions = new ReadOptions();
        when(mLocalDocumentStorage.getDocumentsByPartition(com.microsoft.appcenter.Constants.READONLY_TABLE, APP_DOCUMENTS, readOptions)).thenReturn(new ArrayList<LocalDocument>());
        PaginatedDocuments<String> documents = Data.list(String.class, APP_DOCUMENTS, readOptions).get();
        assertEquals(0, documents.getCurrentPage().getItems().size());
    }

    @Test
    public void listWhenOffline() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(TestDocument.class, USER_DOCUMENTS).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        Page<TestDocument> page = docs.getCurrentPage();
        assertNotNull(page.getItems());
        assertNull(page.getError());
        verifyZeroInteractions(mHttpClient);
        verifyZeroInteractions(mRemoteOperationListener);
        verify(mLocalDocumentStorage).getDocumentsByPartition(startsWith(USER_DOCUMENTS), eq(RESOLVED_USER_PARTITION), any(ReadOptions.class));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        verify(mAuthTokenContext).getAccountId();
        verifyNoMoreInteractions(mAuthTokenContext);
    }

    @Test
    public void listEndToEndWhenSinglePage() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setAccountId(AbstractDataTest.ACCOUNT_ID)
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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
        PaginatedDocuments<TestDocument> docs = Data.list(TestDocument.class, USER_DOCUMENTS).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertEquals(1, docs.getCurrentPage().getItems().size());
        assertEquals(docs.getCurrentPage().getItems().get(0).getDeserializedValue().test, documents.get(0).getDeserializedValue().test);

        /* Verify result was cached */
        ArgumentCaptor<WriteOptions> writeOptions = ArgumentCaptor.forClass(WriteOptions.class);
        verify(mLocalDocumentStorage).writeOnline(
                eq(USER_TABLE_NAME),
                mTestDocumentWrapperCaptor.capture(),
                writeOptions.capture()
        );
        assertNotNull(mTestDocumentWrapperCaptor.getValue());
        assertEquals("document id", mTestDocumentWrapperCaptor.getValue().getId());
        assertNotNull(writeOptions.getValue());
        assertEquals(TimeToLive.DEFAULT, writeOptions.getValue().getDeviceTimeToLive());

        /* Disable the Data module. */
        Data.setEnabled(false).get();

        /* Make the list call again. */
        PaginatedDocuments<TestDocument> docCancel = Data.list(TestDocument.class, USER_DOCUMENTS).get();
        assertNotNull(docCancel);
        assertNull(docCancel.getCurrentPage().getItems());
        assertNotNull(docCancel.getCurrentPage().getError());
        assertEquals(IllegalStateException.class, docCancel.getCurrentPage().getError().getCause().getClass());
    }

    @Test
    public void listEndToEndWhenMultiplePages() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbName("dbName")
                .setDbAccount("accountName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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

        when(mHttpClient.callAsync(endsWith("docs"), anyString(), mHeaders.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                String expectedResponse = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? expectedSecondResponse : expectedFirstResponse;
                Map<String, String> newHeader = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? new HashMap<String, String>() : new HashMap<String, String>() {
                    {
                        put(Constants.CONTINUATION_TOKEN_HEADER, "continuation token");
                    }
                };
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, newHeader);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(TestDocument.class, USER_DOCUMENTS).get();
        assertNull(docs.getCurrentPage().getError());
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
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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

        when(mHttpClient.callAsync(endsWith("docs"), anyString(), mHeaders.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                String expectedResponse = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? expectedSecondResponse : expectedFirstResponse;
                Map<String, String> newHeader = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? new HashMap<String, String>() : new HashMap<String, String>() {
                    {
                        put(Constants.CONTINUATION_TOKEN_HEADER, "continuation token");
                    }
                };
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, newHeader);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        Iterator<DocumentWrapper<TestDocument>> iterator = Data.list(TestDocument.class, USER_DOCUMENTS).get().iterator();
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
    public void listWithEmptyIterator() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Make the call. */
        PaginatedDocuments<TestDocument> paginatedDocuments = Data.list(TestDocument.class, USER_DOCUMENTS).get();
        Iterator<DocumentWrapper<TestDocument>> iterator = paginatedDocuments.iterator();
        assertFalse(iterator.hasNext());
        assertNotNull(paginatedDocuments.getCurrentPage().getError());
        assertNull(paginatedDocuments.getCurrentPage().getItems());

        /* Verify not throws exception. */
        iterator.remove();
    }

    @Test
    public void listOffLineWithContinuationToken() {

        /* Setup mock to get expiration token from cache. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbName("dbName")
                .setDbAccount("accountName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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

        when(mHttpClient.callAsync(endsWith("docs"), anyString(), mHeaders.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                String expectedResponse = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? expectedSecondResponse : expectedFirstResponse;
                Map<String, String> newHeader = mHeaders.getValue().containsKey(Constants.CONTINUATION_TOKEN_HEADER) ? new HashMap<String, String>() : new HashMap<String, String>() {
                    {
                        put(Constants.CONTINUATION_TOKEN_HEADER, "continuation token");
                    }
                };
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, newHeader);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(TestDocument.class, USER_DOCUMENTS).get();
        assertNull(docs.getCurrentPage().getError());
        assertTrue(docs.hasNextPage());
        assertEquals(firstPartDocuments.get(0).getId(), docs.getCurrentPage().getItems().get(0).getId());

        /* Turn the network off before making the second page call. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Page<TestDocument> secondPage = docs.getNextPage().get();
        DataException ex = secondPage.getError();
        assertNotNull(ex);
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Listing next page is not supported in off-line mode."));
    }

    @Test
    public void listEndToEndWhenExceptionHappened() {
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + USER_DOCUMENTS)).thenReturn(tokenResult);
        when(mHttpClient.callAsync(endsWith("docs"), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new Exception("some error"));
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        PaginatedDocuments<TestDocument> docs = Data.list(TestDocument.class, USER_DOCUMENTS).get();

        /* Verify the result correct. */
        assertFalse(docs.hasNextPage());
        assertNotNull(docs.getCurrentPage());
        assertNotNull(docs.getCurrentPage().getError());

        /* Make the call, when continuation token is null. */
        Page nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());

        /* Set the continuation token, but the http call failed. */
        docs.setContinuationToken("fake continuation token").setTokenResult(Utils.getGson().fromJson(tokenResult, TokenResult.class));
        nextPage = docs.getNextPage().get();
        assertNotNull(nextPage);
        assertNotNull(nextPage.getError());
    }

    @Test
    public void listEndToEndWhenMakeTokenExchangeCallFails() throws JSONException {
        AppCenterFuture<PaginatedDocuments<TestDocument>> documents = Data.list(TestDocument.class, USER_DOCUMENTS);

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
    public void listFailsToDeserializeDocumentDoesNotThrow() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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

        when(mHttpClient.callAsync(
                endsWith("docs"),
                anyString(),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                any(ServiceCallback.class)))
                .then(new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                        return mock(ServiceCall.class);
                    }
                });

        /* Make the call. Ensure deserialization error on document by passing incorrect class type. */
        AppCenterFuture<PaginatedDocuments<String>> result = Data.list(String.class, DefaultPartitions.USER_DOCUMENTS);

        verify(mLocalDocumentStorage).getDocumentsByPartition(startsWith(USER_DOCUMENTS), startsWith(USER_DOCUMENTS), any(ReadOptions.class));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        verify(mAuthTokenContext).getAccountId();
        verifyNoMoreInteractions(mAuthTokenContext);

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
    public void listDoUpdateOptions() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbCollectionName("collectionName")
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken"));
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

        when(mHttpClient.callAsync(
                endsWith("docs"),
                anyString(),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                any(ServiceCallback.class)))
                .then(new Answer<ServiceCall>() {

                    @Override
                    public ServiceCall answer(InvocationOnMock invocation) {
                        ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, new HashMap<String, String>());
                        return mock(ServiceCall.class);
                    }
                });

        /* Make the call, ensure the readOptions ttl has been passed to writeOptions. */
        int ttl = 10;
        Data.list(TestDocument.class, DefaultPartitions.USER_DOCUMENTS, new ReadOptions(ttl));
        ArgumentCaptor<WriteOptions> argumentCaptor = ArgumentCaptor.forClass(WriteOptions.class);
        verify(mLocalDocumentStorage).writeOnline(anyString(), any(DocumentWrapper.class), argumentCaptor.capture());
        assertEquals(ttl, argumentCaptor.getValue().getDeviceTimeToLive());
    }

    @Test
    public void listFailsToDeserializeListOfDocumentsDoesNotThrow() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(RESOLVED_USER_PARTITION)
                .setExpirationDate(expirationDate.getTime())
                .setToken("fakeToken")
                .setDbAccount("accountName")
                .setDbName("dbName")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collectionName"));
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
        AppCenterFuture<PaginatedDocuments<TestDocument>> result = Data.list(TestDocument.class, DefaultPartitions.USER_DOCUMENTS);

        /* Verify the result is correct and the cache was not touched. */
        verify(mLocalDocumentStorage).getDocumentsByPartition(startsWith(USER_DOCUMENTS), startsWith(USER_DOCUMENTS), any(ReadOptions.class));
        verifyNoMoreInteractions(mLocalDocumentStorage);
        verify(mAuthTokenContext).getAccountId();
        verifyNoMoreInteractions(mAuthTokenContext);
        assertNotNull(result);
        PaginatedDocuments<TestDocument> docs = result.get();
        assertNotNull(docs);
        assertFalse(docs.hasNextPage());
        Page<TestDocument> page = docs.getCurrentPage();
        assertNotNull(page);
        assertNotNull(page.getError());
    }

    @Test
    public void listUserPartitionReturnsAnExceptionWhenNotSignedIn() {
        mockSignOut();
        PaginatedDocuments<TestDocument> documents = Data.list(TestDocument.class, USER_DOCUMENTS).get();
        Iterator<DocumentWrapper<TestDocument>> iterator = documents.iterator();
        assertFalse(iterator.hasNext());
        assertNotNull(documents);
        Page<TestDocument> currentPage = documents.getCurrentPage();
        assertNotNull(currentPage);
        DataException error = currentPage.getError();
        assertNull(currentPage.getItems());
        assertNotNull(error);
        assertTrue(error.getMessage().contains("List operation requested on user partition, but the user is not logged in."));
    }
}