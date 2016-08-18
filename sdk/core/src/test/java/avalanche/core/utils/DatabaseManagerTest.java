package avalanche.core.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
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
            databaseManagerMock.getScanner(null, null).iterator();
            verify(databaseManagerMock).switchToInMemory(eq("scan.iterator"), any(RuntimeException.class));
        }
        {
            databaseManagerMock = getDatabaseManagerMock();
            databaseManagerMock.getScanner(null, null).getCount();
            verify(databaseManagerMock).switchToInMemory(eq("scan.count"), any(RuntimeException.class));
        }
        {
            /* Cursor next failing but closing working. */
            DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
            databaseManagerMock = spy(databaseManager);
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            when(cursor.moveToNext()).thenThrow(new RuntimeException());
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null);
            assertFalse(scanner.iterator().hasNext());
            verify(databaseManagerMock).switchToInMemory(eq("scan.hasNext"), any(RuntimeException.class));

            /* We switched over in memory so closing will not switch again. Cursor is closed already. */
            doThrow(new RuntimeException()).when(cursor).close();
            scanner.close();
            verify(databaseManagerMock, never()).switchToInMemory(eq("scan.close"), any(RuntimeException.class));
        }
        {
            /* Cursor next failing and closing failing. */
            DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
            databaseManagerMock = spy(databaseManager);
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            when(cursor.moveToNext()).thenThrow(new RuntimeException());
            doThrow(new RuntimeException()).when(cursor).close();
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null);
            assertFalse(scanner.iterator().hasNext());
            verify(databaseManagerMock).switchToInMemory(eq("scan.hasNext"), any(RuntimeException.class));

            /* We switched over in memory so closing will not switch again. Cursor is closed already in hasNext(). */
            scanner.close();
            verify(databaseManagerMock, never()).switchToInMemory(eq("scan.close"), any(RuntimeException.class));
        }
        {
            /* Cursor closing failing. */
            DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
            databaseManagerMock = spy(databaseManager);
            when(databaseManagerMock.getDatabase()).thenReturn(mock(SQLiteDatabase.class));
            mockStatic(SQLiteUtils.class);
            Cursor cursor = mock(Cursor.class);
            SQLiteQueryBuilder sqLiteQueryBuilder = mock(SQLiteQueryBuilder.class, new Returns(cursor));
            when(SQLiteUtils.newSQLiteQueryBuilder()).thenReturn(sqLiteQueryBuilder);
            doThrow(new RuntimeException()).when(cursor).close();
            DatabaseManager.Scanner scanner = databaseManagerMock.getScanner(null, null);
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
    public void deleteException() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.delete(DatabaseManager.PRIMARY_KEY, "non-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getException() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Get. */
        databaseManagerMock.get(DatabaseManager.PRIMARY_KEY, "non-number");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void scannerRemoveInMemoryDB() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Remove. */
        databaseManagerMock.getScanner(null, null).iterator().remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void scannerNextInMemoryDB() {
        DatabaseManager databaseManagerMock;

        /* Switch over to in-memory database. */
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.get(0);

        /* Next. */
        databaseManagerMock.getScanner(null, null).iterator().next();
    }
}