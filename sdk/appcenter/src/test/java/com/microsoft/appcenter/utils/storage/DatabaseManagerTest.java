package com.microsoft.appcenter.utils.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(SQLiteUtils.class)
public class DatabaseManagerTest {

    private static DatabaseManager getDatabaseManagerMock() {

        /* Mocking(spying) instance. */
        DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
        DatabaseManager databaseManagerMock = spy(databaseManager);
        when(databaseManagerMock.getDatabase()).thenThrow(new RuntimeException());
        return databaseManagerMock;
    }

    @Test
    public void switchInMemory() throws Exception {
        DatabaseManager databaseManagerMock;

        /* Put. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.put(new ContentValues());
        verify(databaseManagerMock).switchToInMemory(eq("put"), any(RuntimeException.class));

        /* Update. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.update(0, new ContentValues());
        verify(databaseManagerMock).switchToInMemory(eq("update"), any(RuntimeException.class));

        /* Get. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);
        verify(databaseManagerMock).switchToInMemory(eq("get"), any(RuntimeException.class));

        /* Scanner. */
        {
            databaseManagerMock = getDatabaseManagerMock();
            databaseManagerMock.getScanner(null, null, false).iterator();
            verify(databaseManagerMock).switchToInMemory(eq("scan.iterator"), any(RuntimeException.class));
        }
        {
            databaseManagerMock = getDatabaseManagerMock();
            databaseManagerMock.getScanner(null, null, false).getCount();
            verify(databaseManagerMock).switchToInMemory(eq("scan.count"), any(RuntimeException.class));
        }
        {
            /* Cursor next failing but closing working. */
            databaseManagerMock = spy(new DatabaseManager(null, "database", "table", 1, null, null));
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            when(cursor.moveToNext()).thenThrow(new RuntimeException());
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null, false);
            assertFalse(scanner.iterator().hasNext());
            verify(databaseManagerMock).switchToInMemory(eq("scan.hasNext"), any(RuntimeException.class));

            /* We switched over in memory so closing will not switch again. Cursor is closed already. */
            doThrow(new RuntimeException()).when(cursor).close();
            scanner.close();
            verify(databaseManagerMock, never()).switchToInMemory(eq("scan.close"), any(RuntimeException.class));
        }
        {
            /* Cursor next failing and closing failing. */
            databaseManagerMock = spy(new DatabaseManager(null, "database", "table", 1, null, null));
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            when(cursor.moveToNext()).thenThrow(new RuntimeException());
            doThrow(new RuntimeException()).when(cursor).close();
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null, false);
            assertFalse(scanner.iterator().hasNext());
            verify(databaseManagerMock).switchToInMemory(eq("scan.hasNext"), any(RuntimeException.class));

            /* We switched over in memory so closing will not switch again. Cursor is closed already in hasNext(). */
            scanner.close();
            verify(databaseManagerMock, never()).switchToInMemory(eq("scan.close"), any(RuntimeException.class));
        }
        {
            /* Cursor closing failing. */
            databaseManagerMock = spy(new DatabaseManager(null, "database", "table", 1, null, null));
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            doThrow(new RuntimeException()).when(cursor).close();
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null, false);
            assertFalse(scanner.iterator().hasNext());
            scanner.close();
            verify(databaseManagerMock).switchToInMemory(eq("scan.close"), any(RuntimeException.class));
        }

        /* Delete. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(0);
        verify(databaseManagerMock).switchToInMemory(eq("delete"), any(RuntimeException.class));

        /* Delete multiple IDs. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(new ArrayList<Long>());
        verify(databaseManagerMock, never()).switchToInMemory(eq("delete"), any(RuntimeException.class));
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.delete(Arrays.asList(new Long[]{0L, 1L}));
        verify(databaseManagerMock).switchToInMemory(eq("delete"), any(RuntimeException.class));

        /* Clear. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.clear();
        verify(databaseManagerMock).switchToInMemory(eq("clear"), any(RuntimeException.class));

        /* Close. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.close();
        verify(databaseManagerMock).switchToInMemory(eq("close"), any(RuntimeException.class));

        /* Row count. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.getRowCount();
        verify(databaseManagerMock).switchToInMemory(eq("count"), any(RuntimeException.class));

    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteExceptionWithInvalidValue() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.delete(DatabaseManager.PRIMARY_KEY, "non-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteExceptionWithNullValue() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.delete(DatabaseManager.PRIMARY_KEY, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionWithInvalidValue() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.get(DatabaseManager.PRIMARY_KEY, "non-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionWithNullValue() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.get(DatabaseManager.PRIMARY_KEY, null);
    }

    @Test
    public void updateFailure() {
        /* Update returns 0 or less. */
        DatabaseManager databaseManagerMock = spy(new DatabaseManager(null, "database", "table", 1, null, null));
        SQLiteDatabase sQLiteDatabaseMock = mock(SQLiteDatabase.class);
        when(databaseManagerMock.getDatabase()).thenReturn(sQLiteDatabaseMock);
        when(sQLiteDatabaseMock.update(anyString(), any(ContentValues.class), anyString(), any(String[].class))).thenReturn(-1);

        /* Verify. */
        assertFalse(databaseManagerMock.update(0, new ContentValues()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void scannerRemoveInMemoryDB() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Remove. */
        databaseManagerMock.getScanner(null, null, false).iterator().remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void scannerNextInMemoryDB() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Next. */
        databaseManagerMock.getScanner(null, null, false).iterator().next();
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
    public void inMemoryEviction() {

        /* Mocking instances. */
        Context contextMock = mock(Context.class);

        /* Instantiate real instance for DatabaseManager. */
        DatabaseManager databaseManager = spy(new DatabaseManager(contextMock, "database", "table", 1, null, 2, null));
        databaseManager.switchToInMemory("test", null);

        ContentValues value1 = mock(ContentValues.class);
        ContentValues value2 = mock(ContentValues.class);
        ContentValues value3 = mock(ContentValues.class);

        long value1Id = databaseManager.put(value1);
        verify(value1).put(eq(DatabaseManager.PRIMARY_KEY), anyLong());
        assertEquals(1, databaseManager.getRowCount());

        long value2Id = databaseManager.put(value2);
        verify(value2).put(eq(DatabaseManager.PRIMARY_KEY), anyLong());
        assertEquals(2, databaseManager.getRowCount());

        long value3Id = databaseManager.put(value3);
        verify(value3).put(eq(DatabaseManager.PRIMARY_KEY), anyLong());
        assertEquals(2, databaseManager.getRowCount());

        assertNull(databaseManager.get(value1Id));
        assertNotNull(databaseManager.get(value2Id));
        assertNotNull(databaseManager.get(value3Id));
    }
}