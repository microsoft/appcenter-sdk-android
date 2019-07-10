/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.List;

import static com.microsoft.appcenter.data.LocalDocumentStorage.DOCUMENT_ID_COLUMN_NAME;
import static com.microsoft.appcenter.data.LocalDocumentStorage.PARTITION_COLUMN_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({
        SQLiteUtils.class,
        AppCenterLog.class,
        DatabaseManager.class,
        LocalDocumentStorage.class,
        AuthTokenContext.class})
public class LocalDocumentStorageTest {

    private static final String PARTITION = "partition";

    private static final String DOCUMENT_ID = "id";

    private static final String USER_ID = "12345";

    private static String mUserTableName = Utils.getUserTableName(USER_ID);

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private DatabaseManager mDatabaseManager;

    private LocalDocumentStorage mLocalDocumentStorage;

    private Cursor mCursor;

    private ContentValues mCurrentValue;

    @Mock
    private AuthTokenContext mAuthTokenContext;

    @Before
    public void setUp() throws Exception {
        mockStatic(AuthTokenContext.class);
        when(AuthTokenContext.getInstance()).thenReturn(mAuthTokenContext);
        when(mAuthTokenContext.getAccountId()).thenReturn(USER_ID);
        mockStatic(AppCenterLog.class);
        mDatabaseManager = mock(DatabaseManager.class);
        mCursor = mock(Cursor.class);
        mCurrentValue = mock(ContentValues.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);

        mLocalDocumentStorage = new LocalDocumentStorage(mock(Context.class), null);
    }

    @Test
    public void getTableNameWithUserPartitionName() {
        String tableName = Utils.getTableName(DefaultPartitions.USER_DOCUMENTS, USER_ID);
        assertEquals(String.format(com.microsoft.appcenter.Constants.USER_TABLE_FORMAT, USER_ID), tableName);
    }

    @Test
    public void getTableNameWithReadonlyPartitionName() {
        String tableName = Utils.getTableName(DefaultPartitions.APP_DOCUMENTS, USER_ID);
        assertEquals(com.microsoft.appcenter.Constants.READONLY_TABLE, tableName);
    }

    @Test
    public void validPartitionName() {
        assertTrue(LocalDocumentStorage.isValidPartitionName(DefaultPartitions.USER_DOCUMENTS));
        assertTrue(LocalDocumentStorage.isValidPartitionName(DefaultPartitions.APP_DOCUMENTS));
        assertFalse(LocalDocumentStorage.isValidPartitionName("invalid"));
    }

    @Test
    public void updateGetsCalledInWrite() {
        mLocalDocumentStorage.writeOnline(mUserTableName, new DocumentWrapper<>("Test value", PARTITION, DOCUMENT_ID), new WriteOptions());
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(eq(mUserTableName), argumentCaptor.capture(), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME));
        assertNotNull(argumentCaptor.getValue());
    }

    @Test
    public void getDocumentsByPartitionWhenTableIsNull() {
        List<LocalDocument> list = mLocalDocumentStorage.getDocumentsByPartition(null, DefaultPartitions.USER_DOCUMENTS, new ReadOptions());
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void localStorageDoNotWriteWhenNotCache() {
        mLocalDocumentStorage.writeOffline(mUserTableName, new DocumentWrapper<>("Test", PARTITION, DOCUMENT_ID), new WriteOptions(TimeToLive.NO_CACHE));
        verify(mDatabaseManager, never()).replace(anyString(), any(ContentValues.class));
    }

    @Test
    public void updateGetsCalledInWriteWithPendingOperation() {
        mLocalDocumentStorage.writeOnline(mUserTableName, new DocumentWrapper<>("Test value", PARTITION, DOCUMENT_ID), new WriteOptions());
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(eq(mUserTableName), argumentCaptor.capture(), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME));
        assertNotNull(argumentCaptor.getValue());
    }

    @Test
    public void readWithNoCacheDiscardsPreviousWriteWithCache() {
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        when(mDatabaseManager.nextValues(mCursor)).thenReturn(mCurrentValue);
        when(mCurrentValue.getAsLong(anyString())).thenReturn(-1L);
        when(mCurrentValue.getAsString(anyString())).thenReturn("\"Test value\"");
        DocumentWrapper<String> doc = mLocalDocumentStorage.read(mUserTableName, PARTITION, DOCUMENT_ID, String.class, ReadOptions.createNoCacheOptions());

        /* Verify that we delete the written document because readOptions are set to NoCache. */
        verify(mDatabaseManager).delete(anyString(), any(ContentValues.class));
        assertNotNull(doc);
        assertNotNull(doc.getDeserializedValue());
        assertFalse(doc.hasFailed());
        assertTrue(doc.isFromDeviceCache());
        verify(mCursor).close();
    }

    @Test
    public void readClosesDatabaseCursor() {
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        DocumentWrapper<String> doc = mLocalDocumentStorage.read(mUserTableName, PARTITION, DOCUMENT_ID, String.class, ReadOptions.createNoCacheOptions());
        assertNotNull(doc);
        assertNull(doc.getDeserializedValue());
        assertTrue(doc.hasFailed());
        assertEquals(DataException.class, doc.getError().getClass());
        verify(mCursor).close();
    }

    @Test
    public void readReturnsErrorObjectOnDbRuntimeException() {
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        DocumentWrapper<String> doc = mLocalDocumentStorage.read(mUserTableName, PARTITION, DOCUMENT_ID, String.class, ReadOptions.createNoCacheOptions());
        assertNotNull(doc);
        assertNull(doc.getDeserializedValue());
        assertTrue(doc.hasFailed());
        assertEquals(DataException.class, doc.getError().getClass());
        assertThat(doc.getError().getMessage(), CoreMatchers.containsString(LocalDocumentStorage.FAILED_TO_READ_FROM_CACHE));
    }

    @Test(expected = RuntimeException.class)
    public void cursorThrowsInGetOperations() {
        Cursor cursor = mock(Cursor.class);
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(cursor);
        when(cursor.moveToNext()).thenThrow(new RuntimeException());
        List<LocalDocument> pendingOperations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
    }

    @Test
    public void createOrUpdateReturnErrorOnDbRuntimeException() {
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        DocumentWrapper<String> doc = mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, PARTITION, DOCUMENT_ID, "test", String.class, new WriteOptions());
        assertNotNull(doc);
        assertNull(doc.getDeserializedValue());
        assertTrue(doc.hasFailed());
        assertEquals(DataException.class, doc.getError().getClass());
        assertThat(doc.getError().getMessage(), CoreMatchers.containsString(LocalDocumentStorage.FAILED_TO_READ_FROM_CACHE));
        assertTrue(doc.isFromDeviceCache());
    }

    @Test
    public void createOrUpdateFailedToWriteException() {
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        when(mDatabaseManager.nextValues(mCursor)).thenReturn(null);
        when(mDatabaseManager.replace(anyString(), any(ContentValues.class), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME))).thenReturn(-1L);
        DocumentWrapper<String> doc = mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, PARTITION, DOCUMENT_ID, "test", String.class, new WriteOptions());
        assertNotNull(doc);
        assertNotNull(doc.getError());
        assertTrue(doc.isFromDeviceCache());
    }

    @Test
    public void deleteReturnsErrorObjectOnDbRuntimeException() {
        doThrow(new RuntimeException()).when(mDatabaseManager).delete(anyString(), anyString(), any(String[].class));
        mLocalDocumentStorage.deleteOnline(mUserTableName, PARTITION, DOCUMENT_ID);
        verify(mDatabaseManager).delete(eq(mUserTableName), anyString(), AdditionalMatchers.aryEq(new String[]{PARTITION, DOCUMENT_ID}));
    }

    @Test
    public void deleteDocumentOfflineSucceeds() {
        when(mDatabaseManager.replace(anyString(), any(ContentValues.class), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME))).thenReturn(1L);
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        DocumentWrapper<Void> wrapper = new DocumentWrapper<>(null, PARTITION, DOCUMENT_ID);
        assertTrue(mLocalDocumentStorage.deleteOffline(mUserTableName, wrapper, new WriteOptions()));
        assertTrue(wrapper.isFromDeviceCache());
    }

    @Test
    public void deleteOnlineSucceeds() {
        when(mDatabaseManager.delete(anyString(), anyString(), any(String[].class))).thenReturn(1);
        assertTrue(mLocalDocumentStorage.deleteOnline(mUserTableName, PARTITION, DOCUMENT_ID));
    }

    @Test
    public void deleteOnlineFails() {
        when(mDatabaseManager.delete(anyString(), anyString(), any(String[].class))).thenReturn(0);
        assertFalse(mLocalDocumentStorage.deleteOnline(mUserTableName, PARTITION, DOCUMENT_ID));
    }

    @Test
    public void deleteOfflineFails() {
        when(mDatabaseManager.replace(anyString(), any(ContentValues.class), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME))).thenReturn(-1L);
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        boolean isSuccess = mLocalDocumentStorage.deleteOffline(mUserTableName, PARTITION, DOCUMENT_ID, new WriteOptions());
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(eq(mUserTableName), argumentCaptor.capture(), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME));
        assertNotNull(argumentCaptor.getValue());
        assertFalse(isSuccess);
    }

    @Test
    public void deleteOfflineSucceeds() {
        when(mDatabaseManager.replace(anyString(), any(ContentValues.class), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME))).thenReturn(1L);
        when(mDatabaseManager.getCursor(anyString(), any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        boolean isSuccess = mLocalDocumentStorage.deleteOffline(mUserTableName, PARTITION, DOCUMENT_ID, new WriteOptions());
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(eq(mUserTableName), argumentCaptor.capture(), eq(PARTITION_COLUMN_NAME), eq(DOCUMENT_ID_COLUMN_NAME));
        assertNotNull(argumentCaptor.getValue());
        assertTrue(isSuccess);
    }

    @Test
    public void verifyOptionsConstructors() {
        assertEquals(TimeToLive.INFINITE, ReadOptions.createInfiniteCacheOptions().getDeviceTimeToLive());
        assertEquals(TimeToLive.NO_CACHE, ReadOptions.createNoCacheOptions().getDeviceTimeToLive());
        assertEquals(TimeToLive.INFINITE, WriteOptions.createInfiniteCacheOptions().getDeviceTimeToLive());
        assertEquals(TimeToLive.NO_CACHE, WriteOptions.createNoCacheOptions().getDeviceTimeToLive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyBaseOptionsWithNegativeTtl() {
        ReadOptions readOptions = new ReadOptions(-100);
    }

    @Test
    public void optionsExpirationTest() {
        ReadOptions readOptions = new ReadOptions(2);
        assertTrue(ReadOptions.isExpired(1));
        assertFalse(ReadOptions.isExpired(-1));
    }

    @Test
    public void getPendingOperationsOnNonUserTable() {
        List<LocalDocument> operations = mLocalDocumentStorage.getPendingOperations(null);
        assertNotNull(operations);
        assertEquals(0, operations.size());
    }

    @Test
    public void localDocumentNoPendingOperation(){
        assertFalse(new LocalDocument("Table", null, "partition", "id", "doc", 0, 0, 0).hasPendingOperation());
    }

    @Test
    public void localDocumentStorageDatabaseListenerTests() {
        String userTableName = "UserTable";
        localDocumentStorageDatabaseListenerTest(1, 2, null, false, true);
        localDocumentStorageDatabaseListenerTest(1, 2, userTableName, true, false);
        localDocumentStorageDatabaseListenerTest(2, 3, null, false, true);
        localDocumentStorageDatabaseListenerTest(2, 3, userTableName, false, true);
    }

    private void localDocumentStorageDatabaseListenerTest(int oldVersion, int newVersion, String userTableName, boolean expectTableDrop, boolean expectedOnUpgradeResult) {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        LocalDocumentStorageDatabaseListener listener =
                new LocalDocumentStorageDatabaseListener(userTableName);

        /* Verify no interactions in `onCreate`. */
        listener.onCreate(db);
        verifyZeroInteractions(db);

        /* Verify `onUpgrade`. */
        boolean onUpgradeResult = listener.onUpgrade(db, oldVersion, newVersion);
        if (expectTableDrop) {
            verify(db).execSQL(eq(SQLiteUtils.formatDropTableQuery(userTableName)));
        } else {
            verifyZeroInteractions(db);
        }
        assertEquals(expectedOnUpgradeResult, onUpgradeResult);
    }
}
