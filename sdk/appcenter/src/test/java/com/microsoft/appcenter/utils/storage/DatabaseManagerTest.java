/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SQLiteUtils.class, AppCenterLog.class})
public class DatabaseManagerTest {

    private static DatabaseManager getDatabaseManagerMock() {

        /* Mocking(spying) instance. */
        DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
        DatabaseManager databaseManagerMock = spy(databaseManager);
        when(databaseManagerMock.getDatabase()).thenThrow(new RuntimeException());
        return databaseManagerMock;
    }

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void putFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.put(new ContentValues(), "priority");
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void upsertFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        long result = databaseManagerMock.replace("table", new ContentValues());
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
        assertEquals(-1L, result);
    }

    @Test
    public void upsertCallsReplaceInternally() {
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        long replaceResultId = 768L;
        when(database.replace(anyString(), anyString(), any(ContentValues.class))).thenReturn(replaceResultId);
        when(helperMock.getWritableDatabase()).thenReturn(database);
        String tableName = "table";
        DatabaseManager databaseManager = spy(new DatabaseManager(contextMock, "database", tableName, 1, null, null));
        databaseManager.setSQLiteOpenHelper(helperMock);
        ContentValues contentValues = new ContentValues();
        long result = databaseManager.replace(tableName, contentValues);
        verify(database).replace(eq(tableName), isNull(String.class), refEq(contentValues));
        assertEquals(replaceResultId, result);
    }

    @Test
    public void deleteFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(0);
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void clearFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.clear();
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void closeFailed() {
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        doThrow(new RuntimeException()).when(helperMock).close();
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.setSQLiteOpenHelper(helperMock);
        databaseManagerMock.close();
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void rowCountFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertEquals(-1, databaseManagerMock.getRowCount());
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void setMaxSizeFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertFalse(databaseManagerMock.setMaxSize(1024 * 1024));
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getMaxSizeFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertEquals(-1, databaseManagerMock.getMaxSize());
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getDatabaseFailedThenCleanupFailedThenRetrySucceeded() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        when(helperMock.getWritableDatabase()).thenThrow(new RuntimeException()).thenReturn(mock(SQLiteDatabase.class));
        when(contextMock.deleteDatabase("database")).thenReturn(false);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Get database. */
        SQLiteDatabase database = databaseManager.getDatabase();

        /* Verify. */
        assertNotNull(database);
        verify(contextMock).deleteDatabase("database");
    }

    @Test
    public void getDatabaseFailedThenCleanupFailedThenRetryFailed() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        when(helperMock.getWritableDatabase()).thenThrow(new RuntimeException()).thenReturn(mock(SQLiteDatabase.class));
        when(contextMock.deleteDatabase("database")).thenReturn(true);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Get database. */
        SQLiteDatabase database = databaseManager.getDatabase();

        /* Verify. */
        assertNotNull(database);
        verify(contextMock).deleteDatabase("database");
    }

    @Test(expected = RuntimeException.class)
    public void getDatabaseException() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        when(helperMock.getWritableDatabase()).thenThrow(new RuntimeException()).thenThrow(new RuntimeException());

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Get database. */
        databaseManager.getDatabase();
    }

    @Test
    public void failsToDeleteLogDuringPutWhenFull() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase sqLiteDatabase = mock(SQLiteDatabase.class);
        when(helperMock.getWritableDatabase()).thenReturn(sqLiteDatabase);

        /* Mock the select cursor we are using to find logs to evict to fail. */
        mockStatic(SQLiteUtils.class);
        Cursor cursor = mock(Cursor.class);
        SQLiteDiskIOException fatalException = new SQLiteDiskIOException();
        when(cursor.moveToNext()).thenThrow(fatalException);
        SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
        when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);

        /* Simulate that database is full and that deletes fail because of the cursor. */
        when(sqLiteDatabase.insertOrThrow(anyString(), anyString(), any(ContentValues.class))).thenThrow(new SQLiteFullException());

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* When we put a log, it will fail to purge. */
        assertEquals(-1, databaseManager.put(mock(ContentValues.class), "priority"));
    }

    @Test
    public void cursorFailsToCloseAfterPut() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase sqLiteDatabase = mock(SQLiteDatabase.class);
        when(helperMock.getWritableDatabase()).thenReturn(sqLiteDatabase);

        /* Mock the select cursor we are using to find logs to evict to fail. */
        mockStatic(SQLiteUtils.class);
        Cursor cursor = mock(Cursor.class);
        SQLiteDiskIOException exception = new SQLiteDiskIOException();
        doThrow(exception).when(cursor).close();
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
        when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);

        /* Simulate that database is full only once (will work after purging 1 log). */
        when(sqLiteDatabase.insertOrThrow(anyString(), anyString(), any(ContentValues.class))).thenThrow(new SQLiteFullException()).thenReturn(1L);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* When we put a log, it succeeds even if a problem occurred while closing purge cursor. */
        long id = databaseManager.put(mock(ContentValues.class), "priority");
        assertEquals(1, id);
    }

    @Test
    public void replaceFailsWhileQuerying() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase sqLiteDatabase = mock(SQLiteDatabase.class);
        when(helperMock.getWritableDatabase()).thenReturn(sqLiteDatabase);

        /* Mock the select cursor we are using to find multiple instances to replace to fail. */
        mockStatic(SQLiteUtils.class);
        Cursor cursor = mock(Cursor.class);
        SQLiteDiskIOException fatalException = new SQLiteDiskIOException();
        when(cursor.moveToNext()).thenReturn(true).thenThrow(fatalException);
        SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
        when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* When we put an entry, it will fail to query and thus not replacing. */
        assertEquals(-1, databaseManager.replace("table", mock(ContentValues.class), "someId"));
    }
}