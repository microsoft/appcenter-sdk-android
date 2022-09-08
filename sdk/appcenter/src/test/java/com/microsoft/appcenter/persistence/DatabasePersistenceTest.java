/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.persistence;

import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.utils.storage.DatabaseManager.PRIMARY_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteQueryBuilder;

import androidx.annotation.Nullable;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.persistence.Persistence.PersistenceException;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@PrepareForTest({
        AppCenterLog.class,
        DatabaseManager.class,
        DatabasePersistence.class
})
public class DatabasePersistenceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    /* Create the DatabasePersistence with avoiding scanning large payload files. */
    private DatabasePersistence createDatabasePersistenceInstance(
            Context context,
            @Nullable Integer version,
            @Nullable @SuppressWarnings("SameParameterValue") final ContentValues schema,
            @Nullable DatabaseManager databaseManagerMock,
            @Nullable Cursor cursorMock
    ) throws Exception {
        if (databaseManagerMock == null) {
            databaseManagerMock = mock(DatabaseManager.class);
        }
        if (cursorMock == null) {
            cursorMock = mock(Cursor.class);
        }
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManagerMock);
        when(cursorMock.moveToNext()).thenReturn(false);
        when(databaseManagerMock.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), eq(null))).thenReturn(cursorMock);
        if (version == null || schema == null) {
            return new DatabasePersistence(context);
        } else {
            return new DatabasePersistence(context, version, schema);
        }
    }

    @Test
    public void countLogsWithGetCountException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        Cursor mockCursor = mock(Cursor.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), 1, DatabasePersistence.SCHEMA, mockDatabaseManager, mockCursor);
        when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenReturn(mockCursor);
        when(mockCursor.moveToNext()).thenThrow(new RuntimeException());

        /* Try to get logs count. */
        //noinspection TryFinallyCanBeTryWithResources
        try {
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {

            /* Close. */
            persistence.close();
        }

        /* There is an error log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void clearPendingLogState() throws Exception {

        /* groupCount should be <= 9. */
        final int groupCount = 4;
        final int logCount = 10;

        /* Mock logs. */
        List<List<ContentValues>> list = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            List<ContentValues> iterator = new ArrayList<>();
            for (long l = 1; l <= logCount; l++) {
                ContentValues values = mock(ContentValues.class);
                when(values.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(l + i * logCount);
                when(values.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("{}");
                iterator.add(values);
            }
            list.add(iterator);
        }

        /* Mock instances. */
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        when(mockDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, mockDatabaseManager, null);

        for (int i = 0; i < groupCount; i++) {
            MockCursor mockCursor = new MockCursor(list.get(i));
            mockCursor.mockBuildValues(mockDatabaseManager);
            when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), eq(new String[]{String.valueOf(i)}), anyString()))
                    .thenReturn(mockCursor);
        }

        LogSerializer mockLogSerializer = mock(LogSerializer.class);
        when(mockLogSerializer.deserializeLog(anyString(), any())).thenReturn(mock(Log.class));

        /* Instantiate Database Persistence. */
        persistence.setLogSerializer(mockLogSerializer);

        /* Get logs. */
        for (int i = 0; i < groupCount; i++) {
            persistence.getLogs(String.valueOf(i), Collections.emptyList(), logCount, new ArrayList<>());
        }

        /* Verify there are 4 pending groups. */
        assertEquals(groupCount, persistence.mPendingDbIdentifiersGroups.size());
        assertEquals(groupCount * logCount, persistence.mPendingDbIdentifiers.size());

        /* Clear all pending groups and verify. */
        persistence.clearPendingLogState();
        assertEquals(0, persistence.mPendingDbIdentifiersGroups.size());
        assertEquals(0, persistence.mPendingDbIdentifiers.size());
    }

    @Test
    public void getLogsWithGetCursorException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), 1, DatabasePersistence.SCHEMA, databaseManager, null);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenThrow(new RuntimeException());


        /* Try to get logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithMoveNextException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        Cursor mockCursor = mock(Cursor.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), 1, DatabasePersistence.SCHEMA, databaseManager, mockCursor);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenReturn(mockCursor);
        when(mockCursor.moveToNext()).thenThrow(new RuntimeException());

        /* Try to get logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithGetCorruptedIdsException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);

        /* Make corrupted log. */
        List<ContentValues> fieldValues = new ArrayList<>();
        {
            /* Empty record, "corrupted", cause identifier is null (and no other field either). */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(null);
            fieldValues.add(contentValues);
        }

        /* Mock log sequence retrieved from cursor. */
        MockCursor mockCursor = new MockCursor(fieldValues);
        mockCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString())).thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        Cursor failingCursor = mock(Cursor.class);
        when(failingCursor.moveToNext()).thenThrow(new SQLiteDiskIOException());
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), any())).thenReturn(failingCursor);

        /* Get logs and verify we get only non corrupted logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithCorruption() throws Exception {

        /* Mock instances. */
        int logCount = 3;
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);

        /* Make 3 logs, the second one will be corrupted. */
        List<ContentValues> fieldValues = new ArrayList<>(logCount);
        {
            /* Valid record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(0L);
            when(contentValues.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("first");
            fieldValues.add(contentValues);
        }
        {
            /* Empty record, "corrupted", cause identifier is null (and no other field either). */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(null);
            fieldValues.add(contentValues);
        }
        {
            /* Valid record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(2L);
            when(contentValues.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("last");
            fieldValues.add(contentValues);
        }

        /* Mock log sequence retrieved from cursor. */
        MockCursor mockCursor = new MockCursor(fieldValues);
        mockCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString()))
                .thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        List<ContentValues> idValues = new ArrayList<>(logCount);
        for (long i = 0; i < logCount; i++) {
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(i);
            idValues.add(contentValues);
        }
        MockCursor mockIdCursor = new MockCursor(idValues);
        mockIdCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), isNull()))
                .thenReturn(mockIdCursor);

        /* Mock serializer and eventually the database. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<Log>() {

            @Override
            public Log answer(InvocationOnMock invocation) {

                /* Hack serializer to return type = payload to simplify checking. */
                Log log = mock(Log.class);
                when(log.getType()).thenReturn(invocation.getArgument(0));
                return log;
            }
        });
        persistence.setLogSerializer(logSerializer);

        /* Get logs and verify we get only non corrupted logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(logCount - 1, outLogs.size());
        assertEquals("first", outLogs.get(0).getType());
        assertEquals("last", outLogs.get(1).getType());

        /* Verify we detected and deleted the corrupted log, the second one. */
        verify(databaseManager).delete(1);

        /* Verify next call is empty logs as they are pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(0, outLogs.size());

        /*
         * Add new logs with corruption again. First 2 logs are still there.
         * Also this time the corrupted log will not even return its identifier when scanning
         * with only id fields, to test that the delete fails gracefully and that we can still
         * work with other logs.
         */
        logCount = 4;
        fieldValues = new ArrayList<>(logCount);
        {
            /* Valid record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(0L);
            when(contentValues.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("first");
            fieldValues.add(contentValues);
        }
        {
            /* Valid record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(2L);
            when(contentValues.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("last");
            fieldValues.add(contentValues);
        }
        {
            /* New corrupted record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(null);
            fieldValues.add(contentValues);
        }
        {
            /* Valid new record. */
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(4L);
            when(contentValues.getAsString(DatabasePersistence.COLUMN_LOG)).thenReturn("true last");
            fieldValues.add(contentValues);
        }
        mockCursor = new MockCursor(fieldValues) {

            @Override
            public void close() {

                /* It should be ignored. */
                throw new RuntimeException();
            }
        };
        mockCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString()))
                .thenReturn(mockCursor);
        idValues = new ArrayList<>(4);

        /* Here the id cursor will also skip the new corrupted log which id would be 3. */
        for (long i = 0; i < logCount; i += 2) {
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(i);
            idValues.add(contentValues);
        }
        mockIdCursor = new MockCursor(idValues) {

            @Override
            public void close() {

                /* It should be ignored. */
                throw new RuntimeException();
            }
        };
        mockIdCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), isNull()))
                .thenReturn(mockIdCursor);

        /* Verify next call is only the new valid log as others are marked pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(1, outLogs.size());
        assertEquals("true last", outLogs.get(0).getType());

        /* Verify that the only log we deleted in the entire test was the one from previous test (id=1). */
        verify(databaseManager).delete(anyLong());
    }

    @Test
    public void checkSetStorageSizeForwarding() throws Exception {

        /* The real Android test for checking size is in DatabaseManagerAndroidTest. */
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString()))
                .thenReturn(mock(Cursor.class));
        when(databaseManager.setMaxSize(anyLong())).thenReturn(true).thenReturn(false);

        /* Just checks calls are forwarded to the low level database layer. */
        assertTrue(persistence.setMaxStorageSize(20480));
        assertFalse(persistence.setMaxStorageSize(2));
    }

    @Test(expected = PersistenceException.class)
    public void failsToDeleteLogDuringPutWhenFull() throws Exception {
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        String deserializedLog = "{name:\"MockLog\"}";
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(deserializedLog);
        persistence.setLogSerializer(logSerializer);

        /* Mock the database managers methods. */
        when(databaseManager.getMaxSize()).thenReturn((long) deserializedLog.getBytes(StandardCharsets.UTF_8).length * 3);
        when(databaseManager.deleteTheOldestRecord(anySet(), anyString(), anyInt())).thenReturn(null);
        when(databaseManager.put(any(ContentValues.class))).thenThrow(new SQLiteFullException());

        /* Persist a log. */
        persistence.putLog(mock(Log.class), "mock", NORMAL);
    }

    @Test
    public void catchErrorsWhileFileScanning() throws Exception {
        /* Initialize mocks. */
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        Cursor mockCursor = mock(Cursor.class);
        ContentValues mockContentValues = mock(ContentValues.class);
        File mockLargePayloadDirectory = mock(File.class);
        File mockGroupDirectoryFirst = mock(File.class);
        File mockGroupDirectorySecond = mock(File.class);
        File mockLargePayloadFile = mock(File.class);
        File mockLargePayloadFileWithWrongName = mock(File.class);
        File mockLastedLargePayloadFile = mock(File.class);
        File mockLastedLargePayloadFileWithError = mock(File.class);
        mockStatic(AppCenterLog.class);

        /* Setup behaviour for database manager, cursor and content values mocks. */
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mockDatabaseManager);
        when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), any())).thenReturn(mockCursor);
        when(mockCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mockDatabaseManager.buildValues(mockCursor)).thenReturn(mockContentValues);
        when(mockContentValues.getAsLong(PRIMARY_KEY)).thenReturn(1L);

        /* Setup behaviour for files and directories mocks for searching in the file hierarchy.
         *  There are 5 iterations:
         *   1) Directory without files.
         *   2) File that corresponds with log id from database.
         *   3) File that with name that cannot be converted to number.
         *   4) Lasted log file that should be deleted successfully.
         *   5) Lasted log file that should be deleted unsuccessfully.
         * */
        whenNew(File.class).withAnyArguments().thenReturn(mockLargePayloadDirectory);
        when(mockLargePayloadDirectory.listFiles()).thenReturn(new File[]{mockGroupDirectoryFirst, mockGroupDirectorySecond});
        when(mockLargePayloadFile.getName()).thenReturn("1.json");
        when(mockLargePayloadFileWithWrongName.getName()).thenReturn("someName.json");
        when(mockLastedLargePayloadFile.getName()).thenReturn("2.json");
        when(mockLastedLargePayloadFile.delete()).thenReturn(true);
        when(mockLastedLargePayloadFileWithError.getName()).thenReturn("3.json");
        when(mockLastedLargePayloadFileWithError.delete()).thenReturn(false);
        when(mockGroupDirectoryFirst.listFiles()).thenReturn(null);
        when(mockGroupDirectorySecond.listFiles()).thenReturn(new File[]{mockLargePayloadFile, mockLargePayloadFileWithWrongName, mockLastedLargePayloadFile, mockLastedLargePayloadFileWithError});

        /* Initialize files checking */
        Persistence persistence = new DatabasePersistence(mock(Context.class));

        /* There is an warning log. */
        verifyStatic(AppCenterLog.class, times(2));
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());

        /* Verification of try of file deleting. */
        verify(mockLastedLargePayloadFile).delete();
        verify(mockLastedLargePayloadFileWithError).delete();
    }

    @Test
    public void failedToDeleteLargePayloadFileWhatDoesNotFitMaxSize() throws Exception {
        /* Initialize mocks. */
        mockStatic(AppCenterLog.class);
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        Cursor mockCursor = mock(Cursor.class);
        Persistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, mockDatabaseManager, mockCursor);
        ContentValues mockContentValues = mock(ContentValues.class);
        File mockGroupDirectory = mock(File.class);
        File mockLargePayloadFile = mock(File.class);
        File mockLargePayloadFileWithError = mock(File.class);
        String logFileExtension = ".json";
        String mockGroup = "mockGroup";
        long logId = 1;
        long errorLogId = 2;
        long maxSize = 10;

        /* Setup behaviour for database manager, cursor and content values mocks. */
        when(mockCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mockDatabaseManager.setMaxSize(anyLong())).thenReturn(true);
        when(mockDatabaseManager.getMaxSize()).thenReturn(maxSize);
        when(mockDatabaseManager.getCurrentSize()).thenReturn(maxSize + 2).thenReturn(maxSize + 1).thenReturn(maxSize - 1);
        when(mockDatabaseManager.deleteTheOldestRecord(anySet(), anyString(), anyInt())).thenReturn(mockContentValues);
        when(mockContentValues.getAsLong(PRIMARY_KEY)).thenReturn(logId).thenReturn(errorLogId);
        when(mockContentValues.getAsString(DatabasePersistence.COLUMN_GROUP)).thenReturn(mockGroup);

        /* Setup behaviour for files and directories mocks.
         *  There are 2 files: one with successfully deleting,
         *  one with error while deleting.
         * */
        whenNew(File.class).withAnyArguments().thenReturn(mockGroupDirectory);
        whenNew(File.class).withArguments(mockGroupDirectory, logId + logFileExtension).thenReturn(mockLargePayloadFile);
        whenNew(File.class).withArguments(mockGroupDirectory, errorLogId + logFileExtension).thenReturn(mockLargePayloadFileWithError);
        when(mockLargePayloadFile.exists()).thenReturn(true);
        when(mockLargePayloadFile.delete()).thenReturn(true);
        when(mockLargePayloadFileWithError.exists()).thenReturn(true);
        when(mockLargePayloadFileWithError.delete()).thenReturn(false);

        /* Initialize deleting of logs that not fit max storage size. */
        persistence.setMaxStorageSize(maxSize);

        /* There is an warning log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());

        /* Verification of tries of files deleting. */
        verify(mockLargePayloadFile).delete();
        verify(mockLargePayloadFileWithError).delete();
    }

    @Test(expected = PersistenceException.class)
    public void putLogWithJSONException() throws Exception {
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(Log.class))).thenThrow(new JSONException("JSON exception"));
        persistence.setLogSerializer(logSerializer);

        /* Persist a log. */
        persistence.putLog(mock(Log.class), "test-p1", NORMAL);
    }

    @Test(expected = PersistenceException.class)
    public void putLogWithOpenDatabaseException() throws Exception {
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getMaxSize()).thenReturn(-1L);
        DatabasePersistence persistence = createDatabasePersistenceInstance(mock(Context.class), null, null, databaseManager, null);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(Log.class))).thenReturn("mock");
        persistence.setLogSerializer(logSerializer);

        /* Persist a log. */
        persistence.putLog(mock(Log.class), "test-p1", NORMAL);
    }

    private static class MockCursor extends CursorWrapper {

        private final List<ContentValues> mList;

        private int mIndex = -1;

        private MockCursor(List<ContentValues> list) {
            super(null);
            mList = list;
        }

        @Override
        public boolean moveToNext() {
            return ++mIndex < mList.size();
        }

        @Override
        public void close() {
        }

        private void mockBuildValues(DatabaseManager databaseManager) {
            when(databaseManager.buildValues(eq(this))).then(new Answer<ContentValues>() {

                @Override
                public ContentValues answer(InvocationOnMock invocation) {
                    return mList.get(mIndex);
                }
            });
        }
    }
}
