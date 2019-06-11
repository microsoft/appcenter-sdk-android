/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteQueryBuilder;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.Flags.NORMAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({AppCenterLog.class, DatabaseManager.class, DatabasePersistence.class})
public class DatabasePersistenceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void countLogsForDate() throws Exception {

        /* Expected values. */
        int expectedCount = 1;
        Date expectedDate = new Date();
        String[] expectedColumns = new String[]{"COUNT(*)"};
        String[] expectedWhereArgs = new String[]{String.valueOf(expectedDate.getTime())};

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mockDatabaseManager);
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.getInt(anyInt())).thenReturn(expectedCount);
        when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class), 1, DatabasePersistence.SCHEMA);

        /* Get count. */
        int actualCount = persistence.countLogs(expectedDate);

        /* Verify. */
        assertEquals(expectedCount, actualCount);
        verify(mockDatabaseManager).getCursor(any(SQLiteQueryBuilder.class), eq(expectedColumns), eq(expectedWhereArgs), anyString());
    }

    @Test
    public void countLogsWithGetCountException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mockDatabaseManager);
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.moveToNext()).thenThrow(new RuntimeException());
        when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class), 1, DatabasePersistence.SCHEMA);

        /* Try to get logs count. */
        //noinspection TryFinallyCanBeTryWithResources
        try {
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {

            /* Close. */
            persistence.close();
        }

        /* There is an error log. */
        verifyStatic();
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
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mockDatabaseManager);
        when(mockDatabaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();

        for (int i = 0; i < groupCount; i++) {
            MockCursor mockCursor = new MockCursor(list.get(i));
            mockCursor.mockBuildValues(mockDatabaseManager);
            when(mockDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), eq(new String[]{String.valueOf(i)}), anyString()))
                    .thenReturn(mockCursor);
        }

        LogSerializer mockLogSerializer = mock(LogSerializer.class);
        when(mockLogSerializer.deserializeLog(anyString(), anyString())).thenReturn(mock(Log.class));

        /* Instantiate Database Persistence. */
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));
        persistence.setLogSerializer(mockLogSerializer);

        /* Get logs. */
        for (int i = 0; i < groupCount; i++) {
            persistence.getLogs(String.valueOf(i), Collections.<String>emptyList(), logCount, new ArrayList<Log>(), null, null);
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
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class), 1, DatabasePersistence.SCHEMA);

        /* Try to get logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithMoveNextException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.moveToNext()).thenThrow(new RuntimeException());
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class), 1, DatabasePersistence.SCHEMA);

        /* Try to get logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithGetCorruptedIdsException() throws Exception {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();

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
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        Cursor failingCursor = mock(Cursor.class);
        when(failingCursor.moveToNext()).thenThrow(new SQLiteDiskIOException());
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(String[].class), any(String[].class), anyString())).thenReturn(failingCursor);

        /* Get logs and verify we get only non corrupted logs. */
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
        assertEquals(0, outLogs.size());

        /* There is an error log. */
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getLogsWithCorruption() throws Exception {

        /* Mock instances. */
        int logCount = 3;
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.nextValues(any(Cursor.class))).thenCallRealMethod();

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
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);

        /* Mock second cursor with identifiers only. */
        List<ContentValues> idValues = new ArrayList<>(logCount);
        for (long i = 0; i < logCount; i++) {
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(i);
            idValues.add(contentValues);
        }
        MockCursor mockIdCursor = new MockCursor(idValues);
        mockIdCursor.mockBuildValues(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(String[].class), any(String[].class), anyString())).thenReturn(mockIdCursor);

        /* Mock serializer and eventually the database. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), anyString())).thenAnswer(new Answer<Log>() {

            @Override
            public Log answer(InvocationOnMock invocation) {

                /* Hack serializer to return type = payload to simplify checking. */
                Log log = mock(Log.class);
                when(log.getType()).thenReturn((String) invocation.getArguments()[0]);
                return log;
            }
        });
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));
        persistence.setLogSerializer(logSerializer);

        /* Get logs and verify we get only non corrupted logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
        assertEquals(logCount - 1, outLogs.size());
        assertEquals("first", outLogs.get(0).getType());
        assertEquals("last", outLogs.get(1).getType());

        /* Verify we detected and deleted the corrupted log, the second one. */
        verify(databaseManager).delete(1);

        /* Verify next call is empty logs as they are pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
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
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNull(String[].class), any(String[].class), anyString())).thenReturn(mockCursor);
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
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), isNotNull(String[].class), any(String[].class), anyString())).thenReturn(mockIdCursor);

        /* Verify next call is only the new valid log as others are marked pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", Collections.<String>emptyList(), 50, outLogs, null, null);
        assertEquals(1, outLogs.size());
        assertEquals("true last", outLogs.get(0).getType());

        /* Verify that the only log we deleted in the entire test was the one from previous test (id=1). */
        verify(databaseManager).delete(anyLong());
    }

    @Test
    public void checkSetStorageSizeForwarding() throws Exception {

        /* The real Android test for checking size is in DatabaseManagerAndroidTest. */
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString()))
                .thenReturn(mock(Cursor.class));
        when(databaseManager.setMaxSize(anyLong())).thenReturn(true).thenReturn(false);

        /* Just checks calls are forwarded to the low level database layer. */
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));
        assertTrue(persistence.setMaxStorageSize(20480));
        assertFalse(persistence.setMaxStorageSize(2));
    }

    @Test(expected = PersistenceException.class)
    public void putLogWithJSONException() throws Exception {
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));

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
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(databaseManager);
        when(databaseManager.getMaxSize()).thenReturn(-1L);
        DatabasePersistence persistence = new DatabasePersistence(mock(Context.class));

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
