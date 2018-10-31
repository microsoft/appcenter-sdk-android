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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    public void putFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.put(new ContentValues(), "priority");
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void deleteFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(0);
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void deleteMultipleIDsFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(new ArrayList<Long>());
        verifyStatic(never());
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
        databaseManagerMock.delete(Arrays.asList(0L, 1L));
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void clearFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.clear();
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void closeFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.close();
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void rowCountFailed() {
        mockStatic(AppCenterLog.class);
        DatabaseManager databaseManagerMock;
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.getRowCount();
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void getDatabaseFailedOnce() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);
        SQLiteOpenHelper helperMock = mock(SQLiteOpenHelper.class);
        when(helperMock.getWritableDatabase()).thenThrow(new RuntimeException()).thenReturn(mock(SQLiteDatabase.class));

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
        DatabaseManager databaseManager = spy(new DatabaseManager(contextMock, "database", "table", 1, null, null));
        databaseManager.setSQLiteOpenHelper(helperMock);

        /* When we put a log, it will fail to purge. */
        assertEquals(-1, databaseManager.put(mock(ContentValues.class), "priority"));
    }
}