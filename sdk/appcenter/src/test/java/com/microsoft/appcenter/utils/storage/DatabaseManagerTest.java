/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import static com.microsoft.appcenter.utils.storage.DatabaseManager.PRIMARY_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SQLiteUtils.class, AppCenterLog.class})
public class DatabaseManagerTest {

    private static DatabaseManager getDatabaseManagerMock() {

        /* Mocking(spying) instance. */
        DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null, null);
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
        databaseManagerMock.put(new ContentValues());
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void deleteFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(0);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void clearFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.clear();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void closeFailed() {
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        doThrow(new RuntimeException()).when(helperMock).close();
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.setSQLiteOpenHelper(helperMock);
        databaseManagerMock.close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void rowCountFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertEquals(-1, databaseManagerMock.getRowCount());
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void setMaxSizeFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertFalse(databaseManagerMock.setMaxSize(1024 * 1024));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getMaxSizeFailed() {
        DatabaseManager databaseManagerMock = getDatabaseManagerMock();
        assertEquals(-1, databaseManagerMock.getMaxSize());
        verifyStatic(AppCenterLog.class);
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
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null, null);
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
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null, null);
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
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Get database. */
        databaseManager.getDatabase();
    }

    @Test
    public void failedToDeleteOldestRecord() throws Exception {
        /* Mocking instances. */
        mockStatic(AppCenterLog.class);
        Context contextMock = mock(Context.class);
        Cursor cursorMock = mock(Cursor.class);
        SQLiteQueryBuilder queryBuilderMock = mock(SQLiteQueryBuilder.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase databaseMock = mock(SQLiteDatabase.class);

        /* Setup behaviour of the SQL stuff. */
        whenNew(SQLiteQueryBuilder.class).withNoArguments().thenReturn(queryBuilderMock);
        when(queryBuilderMock.query(any(SQLiteDatabase.class), any(String[].class), eq(null), any(String[].class), eq(null), eq(null), any(String.class))).thenReturn(cursorMock);
        when(helperMock.getWritableDatabase()).thenReturn(databaseMock);

        /* Setup behaviour of the cursor mock. */
        when(cursorMock.moveToNext()).thenReturn(false);
        when(cursorMock.getColumnCount()).thenReturn(0);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Try to delete the oldest log record. */
        databaseManager.deleteTheOldestRecord(new HashSet<>(), "priorityColumn", Flags.NORMAL);

        /* There is an error log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void exceptionWhileDeleteOldestRecord() throws Exception {
        /* Mocking instances. */
        mockStatic(AppCenterLog.class);
        Context contextMock = mock(Context.class);
        Cursor cursorMock = mock(Cursor.class);
        SQLiteQueryBuilder queryBuilderMock = mock(SQLiteQueryBuilder.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        SQLiteDatabase databaseMock = mock(SQLiteDatabase.class);
        ContentValues contentValuesMock = mock(ContentValues.class);

        /* Setup behaviour of the SQL stuff. */
        whenNew(SQLiteQueryBuilder.class).withNoArguments().thenReturn(queryBuilderMock);
        when(queryBuilderMock.query(any(SQLiteDatabase.class), any(String[].class), eq(null), any(String[].class), eq(null), eq(null), any(String.class))).thenReturn(cursorMock);
        when(helperMock.getWritableDatabase()).thenReturn(databaseMock);

        /* Setup behaviour of the cursor mock. */
        when(cursorMock.moveToNext()).thenReturn(true).thenReturn(false);
        when(cursorMock.getColumnCount()).thenReturn(1);
        when(cursorMock.getColumnName(anyInt())).thenReturn(PRIMARY_KEY);
        when(cursorMock.getLong(anyInt())).thenReturn(1L);

        /* Setup behaviour of the content values mock. */
        PowerMockito.whenNew(ContentValues.class).withNoArguments().thenReturn(contentValuesMock);
        when(contentValuesMock.getAsLong(PRIMARY_KEY)).thenThrow(new RuntimeException());

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = new DatabaseManager(contextMock, "database", "table", 1, null, null, null);
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* Try to delete the oldest log record. */
        assertThrows(RuntimeException.class, () -> {
            databaseManager.deleteTheOldestRecord(new HashSet<>(), "priorityColumn", Flags.NORMAL);
        });
    }
}