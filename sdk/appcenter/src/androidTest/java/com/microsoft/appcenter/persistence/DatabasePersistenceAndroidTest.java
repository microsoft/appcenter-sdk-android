package com.microsoft.appcenter.persistence;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.appcenter.AndroidTestUtils;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogWithProperties;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.json.MockLog;
import com.microsoft.appcenter.ingestion.models.json.MockLogFactory;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Data;
import com.microsoft.appcenter.ingestion.models.one.MockCommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.MockCommonSchemaLogFactory;
import com.microsoft.appcenter.persistence.Persistence.PersistenceException;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.StorageHelper;
import com.microsoft.appcenter.utils.storage.StorageHelper.DatabaseStorage.DatabaseScanner;

import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class DatabasePersistenceAndroidTest {

    /**
     * Maximum storage size in bytes for unit test case.
     */
    private static final int MAX_STORAGE_SIZE_IN_BYTES = 20480;

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
        Constants.loadFromContext(sContext);
    }

    @Before
    public void setUp() {

        /* Clean up database. */
        sContext.deleteDatabase(DatabasePersistence.DATABASE);
    }

    private static int getIteratorSize(Iterator iterator) {
        int count = 0;
        for (; iterator.hasNext(); iterator.next())
            count++;
        return count;
    }

    @Test
    public void putLog() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Initial count is 0. */
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Generate a log and persist. */
            Log log = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log);

            /* Count logs. */
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
            assertEquals(1, persistence.countLogs("test-p1"));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putLargeLogAndDeleteAll() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Initial count is 0. */
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Generate a large log and persist. */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 2 * 1024 * 1024;
            StringBuilder largeValue = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                largeValue.append("x");
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("key", largeValue.toString());
            log.setProperties(properties);
            long id = persistence.putLog("test-p1", log);

            /* Count logs. */
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Verify large file. */
            File file = persistence.getLargePayloadFile(persistence.getLargePayloadGroupDirectory("test-p1"), id);
            assertNotNull(file);
            String fileLog = StorageHelper.InternalStorage.read(file);
            assertNotNull(fileLog);
            assertTrue(fileLog.length() >= size);

            /* Delete entire group. */
            persistence.deleteLogs("test-p1");
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Verify file delete and also parent directory since we used group deletion. */
            assertFalse(file.exists());
            assertFalse(file.getParentFile().exists());
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putLargeLogFails() {

        /* Initialize database persistence. */
        String path = Constants.FILES_PATH;
        Constants.FILES_PATH = null;
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Initial count is 0. */
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Generate a large log and persist. */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 2 * 1024 * 1024;
            StringBuilder largeValue = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                largeValue.append("x");
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("key", largeValue.toString());
            log.setProperties(properties);
            persistence.putLog("test-p1", log);
            fail("putLog was expected to fail");
        } catch (Persistence.PersistenceException e) {
            assertTrue(e.getCause() instanceof IOException);

            /* Make sure database entry has been removed. */
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {
            persistence.close();

            /* Restore path. */
            Constants.FILES_PATH = path;
        }
    }

    @Test
    public void putLargeLogFailsToRead() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 1, DatabasePersistence.SCHEMA);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Initial count is 0. */
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Generate a large log and persist. */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 2 * 1024 * 1024;
            StringBuilder largeValue = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                largeValue.append("x");
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("key", largeValue.toString());
            log.setProperties(properties);
            long id = persistence.putLog("test-p1", log);
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Verify large file. */
            File file = persistence.getLargePayloadFile(persistence.getLargePayloadGroupDirectory("test-p1"), id);
            assertNotNull(file);
            String fileLog = StorageHelper.InternalStorage.read(file);
            assertNotNull(fileLog);
            assertTrue(fileLog.length() >= size);

            /* Delete the file. */
            assertTrue(file.delete());

            /* We won't be able to read the log now but persistence should delete the SQLite log on error. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(0, outputLogs.size());
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putLargeLogNotSupportedOnCommonSchema() throws JSONException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Initial count is 0. */
            assertEquals(0, persistence.countLogs("test-p1"));

            /* Generate a large log. */
            CommonSchemaLog log = new MockCommonSchemaLog();
            int size = 2 * 1024 * 1024;
            StringBuilder largeValue = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                largeValue.append("x");
            }
            log.setVer("3.0");
            log.setName("test");
            log.setTimestamp(new Date());
            log.addTransmissionTarget("token");
            Data data = new Data();
            log.setData(data);
            data.getProperties().put("key", largeValue.toString());

            /* Persisting that log should fail. */
            try {
                persistence.putLog("test-p1", log);
                fail("Inserting large common schema log is not supposed to work");
            } catch (PersistenceException e) {

                /* Count logs is still 0 */
                e.printStackTrace();
                assertEquals(0, persistence.countLogs("test-p1"));
            }
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putTooManyLogs() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 2, DatabasePersistence.SCHEMA);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs that will be evicted. */
            for (int i = 0; i < 10; i++) {
                persistence.putLog("test-p1", AndroidTestUtils.generateMockLog());
            }

            /*
             * Generate the maximum number of logs that we can store in this configuration.
             * This will evict all previously stored logs.
             */
            List<Log> expectedLogs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog("test-p1", log);
                expectedLogs.add(log);
            }

            /* Get logs from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 7, outputLogs);
            assertEquals(expectedLogs.size(), persistence.countLogs("test-p1"));
            assertEquals(expectedLogs, outputLogs);
        } finally {

            //noinspection ThrowFromFinallyBlock
            persistence.close();
        }
    }

    @Test
    public void putLogLargerThanMaxSizeClearsEverything() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 2, DatabasePersistence.SCHEMA);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs that will be evicted. */
            int someLogCount = 3;
            for (int i = 0; i < someLogCount; i++) {
                persistence.putLog("test-p1", AndroidTestUtils.generateMockLog());
            }
            assertEquals(someLogCount, persistence.countLogs("test-p1"));

            /*
             * Generate a log that is so large that will empty all the database and
             * eventually fails.
             */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 30 * 1024;
            StringBuilder largeValue = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                largeValue.append("x");
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("key", largeValue.toString());
            log.setProperties(properties);
            long id = persistence.putLog("test-p1", log);

            /* Verify the behavior: not inserted and database now empty. */
            assertEquals(-1, id);
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {

            //noinspection ThrowFromFinallyBlock
            persistence.close();
        }
    }

    @Test(expected = PersistenceException.class)
    public void putLogException() throws PersistenceException, JSONException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        doThrow(new JSONException("JSON exception")).when(logSerializer).serializeLog(any(Log.class));
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate a log and persist. */
            Log log = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log);
        } finally {
            persistence.close();
        }
    }

    @Test
    public void deleteLogs() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 1, DatabasePersistence.SCHEMA);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate a log and persist. */
            Log log1 = AndroidTestUtils.generateMockLog();
            Log log2 = AndroidTestUtils.generateMockLog();
            Log log3 = AndroidTestUtils.generateMockLog();
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p2", log3);
            persistence.putLog("test-p3", log4);
            assertEquals(2, persistence.countLogs("test-p1"));
            assertEquals(1, persistence.countLogs("test-p2"));
            assertEquals(1, persistence.countLogs("test-p3"));

            /* Get a log from persistence. */
            List<Log> outputLogs1 = new ArrayList<>();
            List<Log> outputLogs2 = new ArrayList<>();
            List<Log> outputLogs3 = new ArrayList<>();
            String id = persistence.getLogs("test-p1", Collections.<String>emptyList(), 5, outputLogs1);
            persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs2);
            persistence.getLogs("test-p3", Collections.<String>emptyList(), 5, outputLogs3);

            /* Verify. */
            assertNotNull(id);
            assertNotEquals("", id);
            assertEquals(2, outputLogs1.size());
            assertEquals(1, outputLogs2.size());
            assertEquals(1, outputLogs3.size());

            /* Delete. */
            persistence.deleteLogs("", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner1 = persistence.mDatabaseStorage.getScanner(DatabasePersistence.COLUMN_GROUP, "test-p1");
            DatabaseScanner scanner2 = persistence.mDatabaseStorage.getScanner(DatabasePersistence.COLUMN_GROUP, "test-p2");
            DatabaseScanner scanner3 = persistence.mDatabaseStorage.getScanner(DatabasePersistence.COLUMN_GROUP, "test-p3");

            //noinspection TryFinallyCanBeTryWithResources
            try {

                /* Verify. */
                assertEquals(2, getIteratorSize(scanner1.iterator()));
                assertEquals(1, getIteratorSize(scanner2.iterator()));
                assertEquals(1, getIteratorSize(scanner3.iterator()));
            } finally {

                /* Close. */
                scanner1.close();
                scanner2.close();
                scanner3.close();
            }

            /* Delete. */
            persistence.deleteLogs("test-p1", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner4 = persistence.mDatabaseStorage.getScanner(DatabasePersistence.COLUMN_GROUP, "test-p1");

            //noinspection TryFinallyCanBeTryWithResources
            try {

                /* Verify. */
                assertEquals(0, getIteratorSize(scanner4.iterator()));
            } finally {

                /* Close. */
                scanner4.close();
            }

            /* Count logs after delete. */
            assertEquals(0, persistence.countLogs("test-p1"));
            assertEquals(1, persistence.countLogs("test-p2"));
            assertEquals(1, persistence.countLogs("test-p3"));

        } finally {
            persistence.close();
        }
    }

    @Test
    public void deleteLogsForGroup() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate a log and persist. */
            Log log1 = AndroidTestUtils.generateMockLog();
            Log log2 = AndroidTestUtils.generateMockLog();
            Log log3 = AndroidTestUtils.generateMockLog();
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p2", log3);
            persistence.putLog("test-p3", log4);

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            String id1 = persistence.getLogs("test-p1", Collections.<String>emptyList(), 5, outputLogs);
            String id2 = persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs);
            assertNotNull(id1);
            assertNotNull(id2);

            /* Delete. */
            persistence.deleteLogs("test-p1");
            persistence.deleteLogs("test-p3");

            /* Try another get for verification. */
            outputLogs.clear();
            persistence.getLogs("test-p3", Collections.<String>emptyList(), 5, outputLogs);

            /* Verify. */
            Map<String, List<Long>> pendingGroups = persistence.mPendingDbIdentifiersGroups;
            assertNull(pendingGroups.get("test-p1" + id1));
            assertEquals(1, pendingGroups.get("test-p2" + id2).size());
            assertEquals(1, pendingGroups.size());
            assertEquals(0, outputLogs.size());
            assertEquals(1, persistence.mDatabaseStorage.size());

            /* Verify one log still persists in the database. */
            persistence.clearPendingLogState();
            outputLogs.clear();
            persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log3, outputLogs.get(0));

            /* Count for groups. */
            assertEquals(0, persistence.countLogs("test-p1"));
            assertEquals(1, persistence.countLogs("test-p2"));
            assertEquals(0, persistence.countLogs("test-p3"));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void getLogs() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Test constants. */
            int numberOfLogs = 10;
            int sizeForGetLogs = 4;

            /* Generate and persist some logs. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++) {
                logs[i] = AndroidTestUtils.generateMockLog();
                persistence.putLog("test", logs[i]);
            }

            /* Get. */
            getAllLogs(persistence, numberOfLogs, sizeForGetLogs);

            /* Clear ids, we should be able to get the logs again in the same sequence. */
            persistence.clearPendingLogState();
            getAllLogs(persistence, numberOfLogs, sizeForGetLogs);

            /* Count. */
            assertEquals(10, persistence.countLogs("test"));

            /* Clear. Nothing to get after. */
            persistence.mDatabaseStorage.clear();
            List<Log> outputLogs = new ArrayList<>();
            assertNull(persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs));
            assertTrue(outputLogs.isEmpty());
            assertEquals(0, persistence.countLogs("test"));
        } finally {

            //noinspection ThrowFromFinallyBlock
            persistence.close();
        }
    }

    private void getAllLogs(DatabasePersistence persistence, int numberOfLogs, int sizeForGetLogs) {
        List<Log> outputLogs = new ArrayList<>();
        int expected = 0;
        do {
            numberOfLogs -= expected;
            persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs);
            expected = Math.min(Math.max(numberOfLogs, 0), sizeForGetLogs);
            assertEquals(expected, outputLogs.size());
            outputLogs.clear();
        } while (numberOfLogs > 0);

        /* Get should be 0 now. */
        persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs);
        assertEquals(0, outputLogs.size());
    }

    @Test
    public void getLogsFilteringOutPausedTargetKeys() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MockCommonSchemaLog.TYPE, new MockCommonSchemaLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Test constants. */
            int numberOfLogsPerKey = 10;

            /* Generate and persist some logs with a first iKey. */
            String pausedKey1 = "1";
            generateCsLogsWithIKey(persistence, pausedKey1, numberOfLogsPerKey);

            /* Generate more logs with another iKey to exclude. */
            String pausedKey2 = "2";
            generateCsLogsWithIKey(persistence, pausedKey2, numberOfLogsPerKey);

            /* Generate logs from a third key. */
            String resumedKey = "3";
            generateCsLogsWithIKey(persistence, resumedKey, numberOfLogsPerKey);

            /* Get logs without disabled keys. */
            List<Log> outLogs = new ArrayList<>();
            int limit = numberOfLogsPerKey * 3;
            String batchId = persistence.getLogs("test", Arrays.asList(pausedKey1, pausedKey2), limit, outLogs);
            assertNotNull(batchId);

            /* Verify we get a subset of logs without the disabled keys. */
            assertEquals(numberOfLogsPerKey, outLogs.size());
            assertEquals(limit, persistence.countLogs("test"));
            for (Log log : outLogs) {
                assertTrue(log instanceof CommonSchemaLog);
                assertEquals(resumedKey, ((CommonSchemaLog) log).getIKey());
            }

            /* Calling a second time should return nothing since the batch is in progress. */
            outLogs.clear();
            batchId = persistence.getLogs("test", Arrays.asList(pausedKey1, pausedKey2), limit, outLogs);
            assertNull(batchId);
            assertEquals(0, outLogs.size());

            /* If we try to get a second batch without filtering, we should get all disabled logs. */
            outLogs.clear();
            batchId = persistence.getLogs("test", Collections.<String>emptyList(), limit, outLogs);
            assertNotNull(batchId);
            assertEquals(numberOfLogsPerKey * 2, outLogs.size());
            for (Log log : outLogs) {
                assertTrue(log instanceof CommonSchemaLog);
                assertNotEquals(resumedKey, ((CommonSchemaLog) log).getIKey());
            }
        } finally {

            //noinspection ThrowFromFinallyBlock
            persistence.close();
        }
    }

    /**
     * Utility for getLogsFilteringOutPausedTargetKeys test.
     */
    private void generateCsLogsWithIKey(DatabasePersistence persistence, String iKey, int numberOfLogsPerKey) throws PersistenceException {
        for (int i = 0; i < numberOfLogsPerKey; i++) {
            CommonSchemaLog log = new MockCommonSchemaLog();
            log.setVer("3.0");
            log.setName("test");
            log.setTimestamp(new Date());
            log.setIKey(iKey);
            log.addTransmissionTarget(iKey + "-token");
            persistence.putLog("test", log);
        }
    }

    @Test
    public void getLogsException() throws PersistenceException, JSONException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = spy(new DefaultLogSerializer());

        /* Throw a JSON exception for the first call. */
        doThrow(new JSONException("JSON exception"))
                /* Return a normal log for the second call. */
                .doReturn(AndroidTestUtils.generateMockLog())
                /* Throw a JSON exception for the third call. */
                .doThrow(new JSONException("JSON exception"))
                /* Return a normal log for further calls. */
                .doReturn(AndroidTestUtils.generateMockLog())
                .when(logSerializer).deserializeLog(anyString(), anyString());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Test constants. */
            int numberOfLogs = 4;

            /* Generate a log and persist. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++)
                logs[i] = AndroidTestUtils.generateMockLog();

            /* Put. */
            for (Log log : logs)
                persistence.putLog("test", log);

            /* Get. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 10, outputLogs);
            assertEquals(numberOfLogs / 2, outputLogs.size());
            assertEquals(2, persistence.mDatabaseStorage.size());
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion1to3() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(DatabasePersistence.SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_TOKEN);
        oldSchema.remove(DatabasePersistence.COLUMN_DATA_TYPE);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_KEY);
        StorageHelper.DatabaseStorage databaseStorage = StorageHelper.DatabaseStorage.getDatabaseStorage(DatabasePersistence.DATABASE, DatabasePersistence.TABLE, 1, oldSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        /* Init log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        logSerializer.addLogFactory(MockCommonSchemaLog.TYPE, new MockCommonSchemaLogFactory());

        /* Insert old data before upgrade. */
        Log oldLog = AndroidTestUtils.generateMockLog();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabasePersistence.COLUMN_GROUP, "test");
            contentValues.put(DatabasePersistence.COLUMN_LOG, logSerializer.serializeLog(oldLog));
            databaseStorage.put(contentValues);
        } finally {
            databaseStorage.close();
        }

        /* Upgrade. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);

        /* Prepare a common schema log. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        commonSchemaLog.setName("test");
        commonSchemaLog.setIKey("o:test");
        commonSchemaLog.setTimestamp(new Date());
        commonSchemaLog.setVer("3.0");
        commonSchemaLog.addTransmissionTarget("test-guid");

        /* Check upgrade. */
        try {

            /* Get old data. */
            assertEquals(1, persistence.countLogs("test"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Put new data with token. */
            persistence.putLog("test/one", commonSchemaLog);
        } finally {
            persistence.close();
        }

        /* Get new data after restart. */
        persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);
        try {

            /* Get new data. */
            assertEquals(1, persistence.countLogs("test/one"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = persistence.mDatabaseStorage.get(DatabasePersistence.COLUMN_GROUP, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion2to3() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(DatabasePersistence.SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_KEY);
        StorageHelper.DatabaseStorage databaseStorage = StorageHelper.DatabaseStorage.getDatabaseStorage(DatabasePersistence.DATABASE, DatabasePersistence.TABLE, DatabasePersistence.VERSION_TYPE_API_KEY, oldSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        /* Init log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        logSerializer.addLogFactory(MockCommonSchemaLog.TYPE, new MockCommonSchemaLogFactory());

        /* Insert old data before upgrade. */
        Log oldLog = AndroidTestUtils.generateMockLog();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabasePersistence.COLUMN_GROUP, "test");
            contentValues.put(DatabasePersistence.COLUMN_LOG, logSerializer.serializeLog(oldLog));
            contentValues.put(DatabasePersistence.COLUMN_DATA_TYPE, MOCK_LOG_TYPE);
            databaseStorage.put(contentValues);
        } finally {
            databaseStorage.close();
        }

        /* Upgrade. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);

        /* Prepare a common schema log. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        commonSchemaLog.setName("test");
        commonSchemaLog.setIKey("o:test");
        commonSchemaLog.setTimestamp(new Date());
        commonSchemaLog.setVer("3.0");
        commonSchemaLog.addTransmissionTarget("test-guid");

        /* Check upgrade. */
        try {

            /* Get old data. */
            assertEquals(1, persistence.countLogs("test"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Put new data with token. */
            persistence.putLog("test/one", commonSchemaLog);
        } finally {
            persistence.close();
        }

        /* Get new data after restart. */
        persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);
        try {

            /* Get new data. */
            assertEquals(1, persistence.countLogs("test/one"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = persistence.mDatabaseStorage.get(DatabasePersistence.COLUMN_GROUP, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);
        } finally {
            persistence.close();
        }
    }
}
