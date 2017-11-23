package com.microsoft.appcenter.persistence;

import android.content.ContentValues;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.microsoft.appcenter.persistence.DatabasePersistence.COLUMN_GROUP;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@PrepareForTest({AppCenterLog.class, StorageHelper.DatabaseStorage.class})
public class DatabasePersistenceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void databaseOperationException() throws Persistence.PersistenceException, IOException {

        /* Mock instances. */
        mockStatic(AppCenterLog.class);
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
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(RuntimeException.class));
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

    @Test
    public void getLogsWithCorruption() throws JSONException {

        /* Mock instances. */
        int logCount = 3;
        mockStatic(StorageHelper.DatabaseStorage.class);
        StorageHelper.DatabaseStorage databaseStorage = mock(StorageHelper.DatabaseStorage.class);
        when(StorageHelper.DatabaseStorage.getDatabaseStorage(anyString(), anyString(), anyInt(), any(ContentValues.class),
                anyInt(), any(StorageHelper.DatabaseStorage.DatabaseErrorListener.class))).thenReturn(databaseStorage);

        /* Make 3 logs, the second one will be corrupted. */
        Collection<ContentValues> fieldValues = new ArrayList<>(logCount);
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

        /* Mock log sequence retrieved from scanner. */
        StorageHelper.DatabaseStorage.DatabaseScanner databaseScanner = mock(StorageHelper.DatabaseStorage.DatabaseScanner.class);
        when(databaseStorage.getScanner(anyString(), anyString())).thenReturn(databaseScanner);
        when(databaseStorage.getScanner(anyString(), anyString(), eq(false))).thenReturn(databaseScanner);
        when(databaseScanner.iterator()).thenReturn(fieldValues.iterator());

        /* Mock second scanner with identifiers only. */
        Collection<ContentValues> idValues = new ArrayList<>(logCount);
        for (long i = 0; i < logCount; i++) {
            ContentValues contentValues = mock(ContentValues.class);
            when(contentValues.getAsLong(DatabaseManager.PRIMARY_KEY)).thenReturn(i);
            idValues.add(contentValues);
        }
        StorageHelper.DatabaseStorage.DatabaseScanner idDatabaseScanner = mock(StorageHelper.DatabaseStorage.DatabaseScanner.class);
        when(databaseStorage.getScanner(anyString(), anyString(), eq(true))).thenReturn(idDatabaseScanner);
        when(idDatabaseScanner.iterator()).thenReturn(idValues.iterator());

        /* Mock serializer and eventually the database. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenAnswer(new Answer<Log>() {

            @Override
            public Log answer(InvocationOnMock invocation) throws Throwable {

                /* Hack serializer to return type = payload to simplify checking. */
                Log log = mock(Log.class);
                when(log.getType()).thenReturn((String) invocation.getArguments()[0]);
                return log;
            }
        });
        DatabasePersistence persistence = new DatabasePersistence();
        persistence.setLogSerializer(logSerializer);

        /* Get logs and verify we get only non corrupted logs. */
        ArrayList<Log> outLogs = new ArrayList<>();
        persistence.getLogs("mock", 50, outLogs);
        assertEquals(logCount - 1, outLogs.size());
        assertEquals("first", outLogs.get(0).getType());
        assertEquals("last", outLogs.get(1).getType());

        /* Verify we detected and deleted the corrupted log, the second one. */
        verify(databaseStorage).delete(1);

        /* Verify next call is empty logs as they are pending. */
        outLogs = new ArrayList<>();
        persistence.getLogs("mock", 50, outLogs);
        assertEquals(0, outLogs.size());
    }
}