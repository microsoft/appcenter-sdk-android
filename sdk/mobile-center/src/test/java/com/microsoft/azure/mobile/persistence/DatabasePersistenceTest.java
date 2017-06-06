package com.microsoft.azure.mobile.persistence;

import android.content.ContentValues;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.DatabaseManager;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.mobile.persistence.DatabasePersistence.COLUMN_GROUP;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@PrepareForTest({MobileCenterLog.class, StorageHelper.DatabaseStorage.class})
public class DatabasePersistenceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void databaseOperationException() throws Persistence.PersistenceException, IOException {

        /* Mock instances. */
        mockStatic(MobileCenterLog.class);
        LogSerializer mockSerializer = mock(DefaultLogSerializer.class);
        DatabasePersistence mockPersistence = spy(new DatabasePersistence("test-persistence", "operation.exception", 1));
        doReturn(mockSerializer).when(mockPersistence).getLogSerializer();
        try {
            /* Generate a log and persist. */
            Log log = mock(Log.class);
            mockPersistence.putLog("test-p1", log);
        } finally {
            /* Close. */
            //noinspection ThrowFromFinallyBlock
            mockPersistence.close();
        }

        verifyStatic();
        MobileCenterLog.error(eq(MobileCenter.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test(expected = IOException.class)
    public void persistenceCloseException() throws IOException {

        /* Mock instances. */
        DatabasePersistence mockPersistence = spy(new DatabasePersistence("test-persistence", "operation.exception", 1));
        doThrow(new IOException()).when(mockPersistence).close();

        /* Close. */
        mockPersistence.close();
    }

    @Test
    public void clearPendingLogState() throws JSONException {

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
        mockStatic(StorageHelper.DatabaseStorage.class);
        StorageHelper.DatabaseStorage mockDatabaseStorage = mock(StorageHelper.DatabaseStorage.class);
        when(StorageHelper.DatabaseStorage.getDatabaseStorage(anyString(), anyString(), anyInt(), any(ContentValues.class),
                anyInt(), any(StorageHelper.DatabaseStorage.DatabaseErrorListener.class))).thenReturn(mockDatabaseStorage);

        for (int i = 0; i < groupCount; i++) {
            StorageHelper.DatabaseStorage.DatabaseScanner mockDatabaseScanner = mock(StorageHelper.DatabaseStorage.DatabaseScanner.class);
            when(mockDatabaseScanner.iterator()).thenReturn(list.get(i).iterator());
            when(mockDatabaseStorage.getScanner(COLUMN_GROUP, String.valueOf(i))).thenReturn(mockDatabaseScanner);
        }

        LogSerializer mockLogSerializer = mock(LogSerializer.class);
        when(mockLogSerializer.deserializeLog(anyString())).thenReturn(mock(Log.class));

        /* Instantiate Database Persistence. */
        DatabasePersistence persistence = new DatabasePersistence();
        persistence.setLogSerializer(mockLogSerializer);

        /* Get logs. */
        for (int i = 0; i < groupCount; i++) {
            persistence.getLogs(String.valueOf(i), logCount, new ArrayList<Log>());
        }

        /* Verify there are 4 pending groups. */
        assertEquals(groupCount, persistence.mPendingDbIdentifiersGroups.size());
        assertEquals(groupCount * logCount, persistence.mPendingDbIdentifiers.size());

        /* Clear all pending groups and verify. */
        persistence.clearPendingLogState();
        assertEquals(0, persistence.mPendingDbIdentifiersGroups.size());
        assertEquals(0, persistence.mPendingDbIdentifiers.size());
    }
}