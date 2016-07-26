package avalanche.core.utils;

import android.content.ContentValues;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class DatabaseManagerTest {

    private static DatabaseManager getDatabaseManagerMock() {
        /* Mocking(spying) instance. */
        DatabaseManager databaseManager = new DatabaseManager(null, "database", "table", 1, null, null);
        DatabaseManager databaseManagerMock = spy(databaseManager);
        when(databaseManagerMock.getDatabase()).thenThrow(new RuntimeException());
        return databaseManagerMock;
    }

    @Test
    public void switchInMemory() throws IOException {
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
        databaseManagerMock = getDatabaseManagerMock();
        databaseManagerMock.getScanner(null, null).iterator();
        verify(databaseManagerMock).switchToInMemory(eq("scan"), any(RuntimeException.class));

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

        /* Close. */
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