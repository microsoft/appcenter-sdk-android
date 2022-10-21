/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.persistence;

import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.utils.storage.DatabaseManager.PRIMARY_KEY;
import static com.microsoft.appcenter.persistence.DatabasePersistence.PAYLOAD_MAX_SIZE;
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
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FilenameFilter;
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
@RunWith(PowerMockRunner.class)
public class DatabasePersistenceTest {

    @Rule
    public TemporaryFolder mLargePayloadsFolder = new TemporaryFolder();

    @Mock
    private Context mContext;

    @Mock
    private DatabaseManager mDatabaseManager;

    @Mock
    private Cursor mCursor;

    /* Create the DatabasePersistence with avoiding scanning large payload files. */
    private DatabasePersistence createDatabasePersistenceInstance(
            @Nullable Integer version,
            @Nullable @SuppressWarnings("SameParameterValue") final ContentValues schema
    ) throws Exception {
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);
        when(mCursor.moveToNext()).thenReturn(false);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), eq(null))).thenReturn(mCursor);
        if (version == null || schema == null) {
            return new DatabasePersistence(mContext);
        } else {
            return new DatabasePersistence(mContext, version, schema);
        }
    }

    private DatabasePersistence createDatabasePersistenceInstance() throws Exception {
        return createDatabasePersistenceInstance(null, null);
    }

    @Test
    public void countLogsWithGetCountException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance(1, DatabasePersistence.SCHEMA);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenReturn(mCursor);
        when(mCursor.moveToNext()).thenThrow(new RuntimeException());

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
        when(mDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance();
        for (int i = 0; i < groupCount; i++) {
            MockCursor mockCursor = new MockCursor(list.get(i));
            mockCursor.mockBuildValues(mDatabaseManager);
            when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), eq(new String[]{String.valueOf(i)}), anyString()))
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
        when(mDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(1, DatabasePersistence.SCHEMA);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenThrow(new RuntimeException());

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
        when(mDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance(1, DatabasePersistence.SCHEMA);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString())).thenReturn(mCursor);
        when(mCursor.moveToNext()).thenThrow(new RuntimeException());

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
        when(mDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance();

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
        mockCursor.mockBuildValues(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString())).thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        Cursor failingCursor = mCursor;
        when(failingCursor.moveToNext()).thenThrow(new SQLiteDiskIOException());
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), any())).thenReturn(failingCursor);

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
        when(mDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        DatabasePersistence persistence = createDatabasePersistenceInstance();

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
        mockCursor.mockBuildValues(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString()))
                .thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        List<ContentValues> idValues = new ArrayList<>(logCount);
        for (long i = 0; i < logCount; i++) {
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(i);
            idValues.add(contentValues);
        }
        MockCursor mockIdCursor = new MockCursor(idValues);
        mockIdCursor.mockBuildValues(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), isNull()))
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
        verify(mDatabaseManager).delete(1);

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
        mockCursor.mockBuildValues(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(), any(String[].class), anyString()))
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
        mockIdCursor.mockBuildValues(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(), any(String[].class), isNull()))
                .thenReturn(mockIdCursor);

        /* Verify next call is only the new valid log as others are marked pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.emptyList(), 50, outLogs);
        assertEquals(1, outLogs.size());
        assertEquals("true last", outLogs.get(0).getType());

        /* Verify that the only log we deleted in the entire test was the one from previous test (id=1). */
        verify(mDatabaseManager).delete(anyLong());
    }

    @Test
    public void checkSetStorageSizeForwarding() throws Exception {

        /* The real Android test for checking size is in DatabaseManagerAndroidTest. */
        DatabasePersistence persistence = createDatabasePersistenceInstance();
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), anyString()))
                .thenReturn(mCursor);
        when(mDatabaseManager.setMaxSize(anyLong())).thenReturn(true).thenReturn(false);

        /* Just checks calls are forwarded to the low level database layer. */
        assertTrue(persistence.setMaxStorageSize(20480));
        assertFalse(persistence.setMaxStorageSize(2));
    }

    @Test(expected = PersistenceException.class)
    public void failsToDeleteLogDuringPutWhenFull() throws Exception {
        DatabasePersistence persistence = createDatabasePersistenceInstance();

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        String deserializedLog = "{name:\"MockLog\"}";
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(deserializedLog);
        persistence.setLogSerializer(logSerializer);

        /* Mock the database managers methods. */
        when(mDatabaseManager.getMaxSize()).thenReturn((long) deserializedLog.getBytes(StandardCharsets.UTF_8).length * 3);
        when(mDatabaseManager.deleteTheOldestRecord(anySet(), anyString(), anyInt())).thenReturn(null);
        when(mDatabaseManager.put(any(ContentValues.class))).thenThrow(new SQLiteFullException());

        /* Persist a log and throwing an exception when trying to free space for a new record. */
        persistence.putLog(mock(Log.class), "mock", NORMAL);
    }

    @Test
    public void successfullyFileDeletingWhileFileScanning() throws Exception {
        /* Initialize mocks. */
        ContentValues mockContentValues = mock(ContentValues.class);
        mockStatic(AppCenterLog.class);

        /* Setup behaviour for database manager, cursor and content values mocks. */
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), any())).thenReturn(mCursor);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mDatabaseManager.buildValues(mCursor)).thenReturn(mockContentValues);
        when(mockContentValues.getAsLong(PRIMARY_KEY)).thenReturn(1L);

        /*
         * Setup behaviour for files and directories mocks for searching in the file hierarchy.
         * There are 6 iterations:
         *  1) Directory without files.
         *  2) Redundant file in the large payload directory.
         *  3) File that corresponds with log id from database.
         *  4) File that with name that cannot be converted to number.
         *  5) Lasted log file that should be deleted successfully.
         *  6) Redundant file in the group directory.
         */
        File largePayloadDirectory = mLargePayloadsFolder.getRoot();
        File groupDirectoryFirst = mLargePayloadsFolder.newFolder("group1");
        File groupDirectorySecond = mLargePayloadsFolder.newFolder("group2");
        File redundantFileInLargePayloadDirectory = mLargePayloadsFolder.newFile("someFile.tmp");
        File largePayloadFile = mLargePayloadsFolder.newFile("group2/1.json");
        File largePayloadFileWithWrongName = mLargePayloadsFolder.newFile("group2/someName.json");
        File lastedLargePayloadFile = mLargePayloadsFolder.newFile("group2/2.json");
        File redundantFileInGroupDirectorySecond = mLargePayloadsFolder.newFile("group2/someFile.tmp");
        whenNew(File.class).withAnyArguments().thenReturn(largePayloadDirectory);

        /* Initialize files checking. */
        Persistence persistence = new DatabasePersistence(mContext);

        /* There is an warning log after file with name that cannot be converted to number. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());

        /* There is an debug log after successfully deleting lasted file. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.debug(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void failedToDeleteFileWhileFileScanning() throws Exception {
        /* Initialize mocks. */
        ContentValues contentValues = new ContentValues();
        contentValues.put(PRIMARY_KEY, 1L);
        mockStatic(AppCenterLog.class);
        File mockLargePayloadDirectory = mock(File.class);
        File mockGroupDirectory = mock(File.class);
        File mockLastedLargePayloadFileWithError = mock(File.class);

        /* Setup behaviour for database manager, cursor and content values mocks. */
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(), any(String[].class), any())).thenReturn(mCursor);
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mDatabaseManager.buildValues(mCursor)).thenReturn(contentValues);

        /*
         * Setup behaviour for files and directories mocks for searching in the file hierarchy.
         * There would be the lasted log file that should be deleted unsuccessfully.
         */
        whenNew(File.class).withAnyArguments().thenReturn(mockLargePayloadDirectory);
        when(mockLargePayloadDirectory.listFiles()).thenReturn(new File[]{mockGroupDirectory});
        when(mockLastedLargePayloadFileWithError.getName()).thenReturn("3.json");
        when(mockLastedLargePayloadFileWithError.delete()).thenReturn(false);
        when(mockGroupDirectory.listFiles(any(FilenameFilter.class))).thenReturn(new File[]{mockLastedLargePayloadFileWithError});

        /* Initialize files checking */
        Persistence persistence = new DatabasePersistence(mContext);

        /* Verification of try of file deleting. */
        verify(mockLastedLargePayloadFileWithError).delete();

        /* There is an warning log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void failedToDeleteLargePayloadFileWhatDoesNotFitMaxSize() throws Exception {
        /* Initialize mocks. */
        mockStatic(AppCenterLog.class);
        Persistence persistence = createDatabasePersistenceInstance();
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
        when(mCursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(mDatabaseManager.setMaxSize(anyLong())).thenReturn(true);
        when(mDatabaseManager.getMaxSize()).thenReturn(maxSize);
        when(mDatabaseManager.getCurrentSize()).thenReturn(maxSize + 2).thenReturn(maxSize + 1).thenReturn(maxSize - 1);
        when(mDatabaseManager.deleteTheOldestRecord(anySet(), anyString(), anyInt())).thenReturn(mockContentValues);
        when(mockContentValues.getAsLong(PRIMARY_KEY)).thenReturn(logId).thenReturn(errorLogId);
        when(mockContentValues.getAsString(DatabasePersistence.COLUMN_GROUP)).thenReturn(mockGroup);

        /*
         * Setup behaviour for files and directories mocks.
         * There are 2 files: one with successfully deleting,
         * one with error while deleting.
         */
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
        DatabasePersistence persistence = createDatabasePersistenceInstance();

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(Log.class))).thenThrow(new JSONException("JSON exception"));
        persistence.setLogSerializer(logSerializer);

        /* Persist a log. */
        persistence.putLog(mock(Log.class), "test-p1", NORMAL);
    }

    @Test(expected = PersistenceException.class)
    public void putLogWithOpenDatabaseException() throws Exception {
        when(mDatabaseManager.getMaxSize()).thenReturn(-1L);
        DatabasePersistence persistence = createDatabasePersistenceInstance();

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(Log.class))).thenReturn("mock");
        persistence.setLogSerializer(logSerializer);

        /* Persist a log. */
        persistence.putLog(mock(Log.class), "test-p1", NORMAL);
    }

    @Test(expected = PersistenceException.class)
    public void putLargePayloadWhatDoNotFitMaxSizeFailed() throws Exception {
        mockStatic(AppCenterLog.class);
        DatabasePersistence persistence = createDatabasePersistenceInstance();
        File mockLargePayloadFile = mock(File.class);
        ContentValues mockContentValues = mock(ContentValues.class);

        /* Set a mock of database manager. */
        when(mDatabaseManager.getMaxSize()).thenReturn(PAYLOAD_MAX_SIZE + 2L);
        when(mDatabaseManager.getCurrentSize()).thenReturn(2L);
        when(mDatabaseManager.deleteTheOldestRecord(anySet(), anyString(), anyInt())).thenReturn(mockContentValues).thenReturn(null);

        /* Set a mock payload. */
        byte[] array = new byte[PAYLOAD_MAX_SIZE + 1];
        String payloadMock = new String(array, StandardCharsets.UTF_8);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(payloadMock);
        persistence.setLogSerializer(logSerializer);

        /* Setup mock for the firs successful try of deleting the oldest log record. */
        when(mockContentValues.getAsLong(PRIMARY_KEY)).thenReturn(1L);
        when(mockContentValues.getAsString(DatabasePersistence.COLUMN_GROUP)).thenReturn("mockGroup");
        whenNew(File.class).withAnyArguments().thenReturn(mockLargePayloadFile);
        when(mockLargePayloadFile.exists()).thenReturn(false);

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
