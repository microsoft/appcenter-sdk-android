package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.storage.models.BaseOptions;
import com.microsoft.appcenter.storage.models.DataStoreEventListener;
import com.microsoft.appcenter.storage.models.DocumentError;
import com.microsoft.appcenter.storage.models.DocumentMetadata;
import com.microsoft.appcenter.storage.models.PendingOperation;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_REPLACE_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

public class NetworkStateChangeStorageTest extends AbstractStorageTest {

    @Mock
    private DataStoreEventListener mDataStoreEventListener;

    @Test
    public void pendingCreateOperationSuccess() {
        final PendingOperation pendingOperation = new PendingOperation(
                PENDING_OPERATION_CREATE_VALUE,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});
        ArgumentCaptor<DocumentMetadata> documentMetadataArgumentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);

        Storage.setDataStoreRemoteOperationListener(mDataStoreEventListener);
        mStorage.onNetworkStateUpdated(true);

        verifyTokenExchangeToCosmosDbFlow(null, METHOD_POST, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);

        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(PENDING_OPERATION_CREATE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DocumentError.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mDataStoreEventListener);

        assertEquals(DOCUMENT_ID, documentMetadata.getDocumentId());
        assertEquals(PARTITION, documentMetadata.getPartition());
        assertEquals(ETAG, documentMetadata.getEtag());

        ArgumentCaptor<PendingOperation> pendingOperationCaptor = ArgumentCaptor.forClass(PendingOperation.class);
        verify(mLocalDocumentStorage).updateLocalCopy(pendingOperationCaptor.capture());
        PendingOperation capturedOperation = pendingOperationCaptor.getValue();
        assertNotNull(capturedOperation);
        assertEquals(pendingOperation, capturedOperation);
        assertEquals(ETAG, capturedOperation.getEtag());
        assertEquals(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, capturedOperation.getDocument());

        verifyNoMoreInteractions(mHttpClient);
    }

    @Test
    public void pendingCreateOperationSuccessWithNoListener() {
        final PendingOperation pendingOperation = new PendingOperation(
                PENDING_OPERATION_CREATE_VALUE,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});

        mStorage.onNetworkStateUpdated(true);
        verifyTokenExchangeToCosmosDbFlow(null, METHOD_POST, COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, null);

        ArgumentCaptor<PendingOperation> pendingOperationCaptor = ArgumentCaptor.forClass(PendingOperation.class);
        verify(mLocalDocumentStorage).updateLocalCopy(pendingOperationCaptor.capture());
        PendingOperation capturedOperation = pendingOperationCaptor.getValue();
        assertNotNull(capturedOperation);
        assertEquals(pendingOperation, capturedOperation);
        assertEquals(ETAG, capturedOperation.getEtag());
        assertEquals(COSMOS_DB_DOCUMENT_RESPONSE_PAYLOAD, capturedOperation.getDocument());

        verifyNoMoreInteractions(mHttpClient);
        verifyZeroInteractions(mDataStoreEventListener);
    }

    @Test
    public void pendingReplaceOperationFailure() {
        verifyPendingOperationFailure(PENDING_OPERATION_REPLACE_VALUE, METHOD_POST, null);
    }

    @Test
    public void pendingDeleteOperationFailure() {
        verifyPendingOperationFailure(PENDING_OPERATION_DELETE_VALUE, METHOD_DELETE, DOCUMENT_ID);
    }

    private void verifyPendingOperationFailure(String operation, String cosmosDbMethod, String documentId) {
        final String document = "document";
        final PendingOperation pendingOperation =
                new PendingOperation(
                        operation,
                        PARTITION,
                        DOCUMENT_ID,
                        document,
                        BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});
        ArgumentCaptor<DocumentError> documentErrorArgumentCaptor = ArgumentCaptor.forClass(DocumentError.class);

        Storage.setDataStoreRemoteOperationListener(mDataStoreEventListener);
        mStorage.onNetworkStateUpdated(true);

        HttpException cosmosFailureException = new HttpException(500, "You failed!");
        verifyTokenExchangeToCosmosDbFlow(documentId, cosmosDbMethod, null, cosmosFailureException);

        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(operation),
                isNull(DocumentMetadata.class),
                documentErrorArgumentCaptor.capture());
        DocumentError documentError = documentErrorArgumentCaptor.getValue();
        assertNotNull(documentError);
        verifyNoMoreInteractions(mDataStoreEventListener);

        assertEquals(cosmosFailureException, documentError.getError().getCause());

        ArgumentCaptor<PendingOperation> pendingOperationCaptor = ArgumentCaptor.forClass(PendingOperation.class);
        verify(mLocalDocumentStorage).updateLocalCopy(pendingOperationCaptor.capture());
        PendingOperation capturedOperation = pendingOperationCaptor.getValue();
        assertNotNull(capturedOperation);
        assertEquals(pendingOperation, capturedOperation);
        assertNull(capturedOperation.getEtag());
        assertEquals(document, capturedOperation.getDocument());

        verifyNoMoreInteractions(mHttpClient);
    }

    @Test
    public void unsupportedPendingOperation() {
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(new PendingOperation(
                            "Order a coffee",
                            PARTITION,
                            DOCUMENT_ID,
                            "document",
                            BaseOptions.DEFAULT_ONE_HOUR));
                }});
        mStorage.onNetworkStateUpdated(true);
        verifyZeroInteractions(mHttpClient);
        verifyZeroInteractions(mDataStoreEventListener);
    }

    @Test
    public void networkGoesOfflineDoesNothing() {
        mStorage.onNetworkStateUpdated(false);
        verifyZeroInteractions(mHttpClient);
        verifyZeroInteractions(mLocalDocumentStorage);
        verifyZeroInteractions(mDataStoreEventListener);
    }

    @Test
    public void tokenExchangeCallFailsOnDelete() {
        verifyTokenExchangeCallFails(PENDING_OPERATION_DELETE_VALUE);
    }

    @Test
    public void tokenExchangeCallFailsOnCreateOrUpdate() {
        verifyTokenExchangeCallFails(PENDING_OPERATION_CREATE_VALUE);
    }

    private void verifyTokenExchangeCallFails(String operation) {
        final PendingOperation pendingOperation = new PendingOperation(
                operation,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});

        Storage.setDataStoreRemoteOperationListener(mDataStoreEventListener);
        mStorage.onNetworkStateUpdated(true);
        verityTokenExchangeFlow(null, new Exception("Yeah, it failed."));

        ArgumentCaptor<DocumentError> documentErrorArgumentCaptor = ArgumentCaptor.forClass(DocumentError.class);
        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(operation),
                isNull(DocumentMetadata.class),
                documentErrorArgumentCaptor.capture());
        DocumentError documentError = documentErrorArgumentCaptor.getValue();
        assertNotNull(documentError);
        verifyNoMoreInteractions(mDataStoreEventListener);
    }

    @Test
    public void pendingDeleteOperationSuccess() {
        final PendingOperation pendingOperation = new PendingOperation(
                PENDING_OPERATION_DELETE_VALUE,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});
        ArgumentCaptor<DocumentMetadata> documentMetadataArgumentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);

        Storage.setDataStoreRemoteOperationListener(mDataStoreEventListener);
        mStorage.onNetworkStateUpdated(true);

        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, METHOD_DELETE, "", null);

        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(PENDING_OPERATION_DELETE_VALUE),
                documentMetadataArgumentCaptor.capture(),
                isNull(DocumentError.class));
        DocumentMetadata documentMetadata = documentMetadataArgumentCaptor.getValue();
        assertNotNull(documentMetadata);
        verifyNoMoreInteractions(mDataStoreEventListener);

        assertEquals(DOCUMENT_ID, documentMetadata.getDocumentId());
        assertEquals(PARTITION, documentMetadata.getPartition());
        assertNull(documentMetadata.getEtag());

        verify(mLocalDocumentStorage).updateLocalCopy(eq(pendingOperation));
    }

    @Test
    public void pendingDeleteOperationWithConflict() {
        final PendingOperation pendingOperation = new PendingOperation(
                PENDING_OPERATION_DELETE_VALUE,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});
        ArgumentCaptor<DocumentError> documentErrorArgumentCaptor = ArgumentCaptor.forClass(DocumentError.class);

        Storage.setDataStoreRemoteOperationListener(mDataStoreEventListener);
        mStorage.onNetworkStateUpdated(true);

        HttpException cosmosFailureException = new HttpException(409, "Conflict");
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, METHOD_DELETE, null, cosmosFailureException);

        verify(mDataStoreEventListener).onDataStoreOperationResult(
                eq(pendingOperation.getOperation()),
                isNull(DocumentMetadata.class),
                documentErrorArgumentCaptor.capture());
        DocumentError documentError = documentErrorArgumentCaptor.getValue();
        assertNotNull(documentError);
        verifyNoMoreInteractions(mDataStoreEventListener);

        assertEquals(cosmosFailureException, documentError.getError().getCause());

        verify(mLocalDocumentStorage).delete(eq(pendingOperation));
    }

    @Test
    public void pendingDeleteOperationWithConflictNoListener() {
        final PendingOperation pendingOperation = new PendingOperation(
                PENDING_OPERATION_DELETE_VALUE,
                PARTITION,
                DOCUMENT_ID,
                "document",
                BaseOptions.DEFAULT_ONE_HOUR);
        when(mLocalDocumentStorage.getPendingOperations()).thenReturn(
                new ArrayList<PendingOperation>() {{
                    add(pendingOperation);
                }});

        mStorage.onNetworkStateUpdated(true);

        HttpException cosmosFailureException = new HttpException(409, "Conflict");
        verifyTokenExchangeToCosmosDbFlow(DOCUMENT_ID, METHOD_DELETE, null, cosmosFailureException);

        verify(mLocalDocumentStorage).delete(eq(pendingOperation));
    }
}
