/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.persistence;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.appcenter.AndroidTestUtils;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.Flags;
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
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static com.microsoft.appcenter.persistence.DatabasePersistence.SCHEMA;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    private static final int MAX_STORAGE_SIZE_IN_BYTES = 32 * 1024;

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getTargetContext();
        FileManager.initialize(sContext);
        SharedPreferencesManager.initialize(sContext);
        Constants.loadFromContext(sContext);
    }

    @Before
    public void setUp() {

        /* Clean up database. */
        sContext.deleteDatabase(DatabasePersistence.DATABASE);
    }

    @NonNull
    private ContentValues getContentValues(DatabasePersistence persistence, String group) {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(DatabasePersistence.COLUMN_GROUP + " = ?");
        String[] selectionArgs = new String[]{group};
        Cursor cursor = persistence.mDatabaseManager.getCursor(builder, null, selectionArgs, null);
        ContentValues values = persistence.mDatabaseManager.nextValues(cursor);
        assertNotNull(values);
        return values;
    }

    private void putLog(int inputFlags, Integer persistedPriorityFlag) throws PersistenceException {

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
            persistence.putLog(log, "test-p1", inputFlags);

            /* Count logs. */
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Check priority flag. */
            ContentValues contentValues = getContentValues(persistence, "test-p1");
            assertEquals(persistedPriorityFlag, contentValues.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putLogDefaultFlags() throws PersistenceException {
        putLog(Flags.DEFAULTS, Flags.NORMAL);
    }

    @Test
    public void putLogNormal() throws PersistenceException {
        putLog(Flags.NORMAL, Flags.NORMAL);
    }

    @Test
    public void putLogCritical() throws PersistenceException {
        putLog(Flags.CRITICAL, Flags.CRITICAL);
    }

    @Test
    public void putLogCriticalPlusOtherFlags() throws PersistenceException {
        putLog(Flags.CRITICAL | 0x0300, Flags.CRITICAL);
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
            long id = persistence.putLog(log, "test-p1", NORMAL);

            /* Count logs. */
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Verify large file. */
            File file = persistence.getLargePayloadFile(persistence.getLargePayloadGroupDirectory("test-p1"), id);
            assertNotNull(file);
            String fileLog = FileManager.read(file);
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
            persistence.putLog(log, "test-p1", NORMAL);
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
            long id = persistence.putLog(log, "test-p1", NORMAL);
            assertEquals(1, persistence.countLogs("test-p1"));

            /* Verify large file. */
            File file = persistence.getLargePayloadFile(persistence.getLargePayloadGroupDirectory("test-p1"), id);
            assertNotNull(file);
            String fileLog = FileManager.read(file);
            assertNotNull(fileLog);
            assertTrue(fileLog.length() >= size);

            /* Delete the file. */
            assertTrue(file.delete());

            /* We won't be able to read the log now but persistence should delete the SQLite log on error. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), 1, outputLogs, null, null);
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
                persistence.putLog(log, "test-p1", NORMAL);
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
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate logs until we notice eviction. */
            List<Log> allLogs = new ArrayList<>();
            String group = "test-p1";
            for (int i = 0; allLogs.size() == persistence.countLogs(group); i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog(log, group, NORMAL);
                allLogs.add(log);

                /* Fail if no eviction happens after a long time to avoid infinite loop on bug. */
                assertTrue("No eviction is happening", i < 1000);
            }

            /* When eviction happened it can be 1 or more logs, but deleted logs should be first ones. */
            int databaseCount = persistence.countLogs(group);
            List<Log> expectedLogs = allLogs.subList(allLogs.size() - databaseCount, allLogs.size());

            /* Get logs from persistence and check we have all the most recent logs. */
            List<Log> actualLogs = new ArrayList<>();
            persistence.getLogs(group, Collections.<String>emptyList(), allLogs.size(), actualLogs, null, null);
            assertEquals(expectedLogs, actualLogs);
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putTooManyLogsMixedPriorities() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate 2 critical logs to be kept first. */
            ArrayList<Log> expectedLogs = new ArrayList<>();
            int criticalLogCount = 2;
            for (int i = 0; i < criticalLogCount; i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog(log, "test-p1", CRITICAL);
                expectedLogs.add(log);
            }

            /* Generate normal priority logs until we notice eviction. */
            List<Log> allNormalLogs = new ArrayList<>();
            String group = "test-p1";
            for (int i = 0; allNormalLogs.size() + criticalLogCount == persistence.countLogs(group); i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog(log, group, NORMAL);
                allNormalLogs.add(log);

                /* Fail if no eviction happens after a long time to avoid infinite loop on bug. */
                assertTrue("No eviction is happening", i < 1000);
            }

            /* When eviction happened it can be 1 or more logs, but deleted logs should be first normal ones. */
            int databaseCount = persistence.countLogs(group);
            int normalLogsStartIndex = allNormalLogs.size() - databaseCount + criticalLogCount;
            expectedLogs.addAll(allNormalLogs.subList(normalLogsStartIndex, allNormalLogs.size()));

            /* Get logs from persistence and check we have all the most recent logs. */
            List<Log> actualLogs = new ArrayList<>();
            persistence.getLogs(group, Collections.<String>emptyList(), 2000, actualLogs, null, null);
            assertEquals(expectedLogs, actualLogs);
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putLogTooLargeIsNotEvenTried() throws PersistenceException {

        /* Initialize database persistence. */
        int someLogCount = 3;
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs that will be evicted. */
            for (int i = 0; i < someLogCount; i++) {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", NORMAL);
            }
            assertEquals(someLogCount, persistence.countLogs("test-p1"));

            /* Generate a log that is so large that it eventually fails. */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 32 * 1024;
            Map<String, String> properties = new HashMap<>();
            properties.put("key", generateString(size, 'x'));
            log.setProperties(properties);
            try {
                persistence.putLog(log, "test-p1", NORMAL);
                fail("putLog was expected to fail");
            } catch (PersistenceException e) {

                /* Verify the behavior: not inserted and database isn't empty. */
                assertEquals(someLogCount, persistence.countLogs("test-p1"));
            }
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putNormalLogCloseToMaxSizeClearsEverything() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs of both priority that will be evicted. */
            int someLogCount = 3;
            for (int i = 0; i < someLogCount; i++) {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", NORMAL);
            }
            assertEquals(someLogCount, persistence.countLogs("test-p1"));

            /*
             * Generate a log that is so large that will empty all the database and
             * eventually fails because close to the limit we check and the overhead of columns/index
             * is larger than max size.
             */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 30 * 1024;
            Map<String, String> properties = new HashMap<>();
            properties.put("key", generateString(size, 'x'));
            log.setProperties(properties);
            try {
                persistence.putLog(log, "test-p1", NORMAL);
                fail("Expected persistence exception");
            } catch (PersistenceException ignore) {
            }

            /* Verify the behavior: not inserted and database now empty. */
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putCriticalLogCloseToMaxSizeClearsEverything() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs of both priority that will be evicted. */
            int someLogCount = 3;
            for (int i = 0; i < someLogCount; i++) {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", NORMAL);
            }
            for (int i = 0; i < someLogCount; i++) {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", CRITICAL);
            }
            assertEquals(someLogCount * 2, persistence.countLogs("test-p1"));

            /*
             * Generate a log that is so large that will empty all the database and
             * eventually fails because close to the limit we check and the overhead of columns/index
             * is larger than max size.
             */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 30 * 1024;
            Map<String, String> properties = new HashMap<>();
            properties.put("key", generateString(size, 'x'));
            log.setProperties(properties);
            try {
                persistence.putLog(log, "test-p1", CRITICAL);
                fail("Expected persistence exception");
            } catch (PersistenceException ignore) {
            }

            /* Verify the behavior: not inserted and database now empty. */
            assertEquals(0, persistence.countLogs("test-p1"));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putNormalLogCloseToMaxSizeKeepsCritical() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Generate some logs that will be kept as they are critical. */
            List<Log> expectedLogs = new ArrayList<>();
            int someLogCount = 3;
            for (int i = 0; i < someLogCount; i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog(log, "test-p1", CRITICAL);
                expectedLogs.add(log);
            }
            assertEquals(someLogCount, persistence.countLogs("test-p1"));

            /* Generate some normal priority logs that will be evicted. */
            for (int i = 0; i < 20; i++) {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", NORMAL);
            }

            /*
             * Generate a normal priority log that is so large that will fail but not discard
             * critical logs.
             */
            LogWithProperties log = AndroidTestUtils.generateMockLog();
            int size = 30 * 1024;
            Map<String, String> properties = new HashMap<>();
            properties.put("key", generateString(size, 'x'));
            log.setProperties(properties);
            try {
                persistence.putLog(log, "test-p1", NORMAL);
                fail("Expected persistence exception");
            } catch (PersistenceException ignore) {
            }

            /* Get logs from persistence: critical were kept. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), expectedLogs.size() + 1, outputLogs, null, null);
            assertTrue(expectedLogs.size() >= persistence.countLogs("test-p1"));
            assertThat(expectedLogs, hasItems(outputLogs.toArray(new Log[0])));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void putNormalLogFailsIfFullOfCritical() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        assertTrue(persistence.setMaxStorageSize(MAX_STORAGE_SIZE_IN_BYTES));

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Fill storage with critical logs. */
            List<Log> expectedLogs = new ArrayList<>();
            int someLogCount = 12;
            for (int i = 0; i < someLogCount; i++) {
                MockLog log = AndroidTestUtils.generateMockLog();
                persistence.putLog(log, "test-p1", CRITICAL);
                expectedLogs.add(log);
            }
            assertEquals(someLogCount, persistence.countLogs("test-p1"));

            /* Try to insert a normal log: that will fail and no log deleted. */
            try {
                persistence.putLog(AndroidTestUtils.generateMockLog(), "test-p1", NORMAL);
                fail("Put log was supposed to fail.");
            } catch (PersistenceException ignore) {
            }

            /* Get logs from persistence: critical were kept. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", Collections.<String>emptyList(), expectedLogs.size() + 1, outputLogs, null, null);
            assertTrue(expectedLogs.size() >= persistence.countLogs("test-p1"));
            assertThat(expectedLogs, hasItems(outputLogs.toArray(new Log[0])));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void deleteLogs() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext, 1, SCHEMA);

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
            persistence.putLog(log1, "test-p1", NORMAL);
            persistence.putLog(log2, "test-p1", NORMAL);
            persistence.putLog(log3, "test-p2", NORMAL);
            persistence.putLog(log4, "test-p3", NORMAL);
            assertEquals(2, persistence.countLogs("test-p1"));
            assertEquals(1, persistence.countLogs("test-p2"));
            assertEquals(1, persistence.countLogs("test-p3"));

            /* Get a log from persistence. */
            List<Log> outputLogs1 = new ArrayList<>();
            List<Log> outputLogs2 = new ArrayList<>();
            List<Log> outputLogs3 = new ArrayList<>();
            String id = persistence.getLogs("test-p1", Collections.<String>emptyList(), 5, outputLogs1, null, null);
            persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs2, null, null);
            persistence.getLogs("test-p3", Collections.<String>emptyList(), 5, outputLogs3, null, null);

            /* Verify. */
            assertNotNull(id);
            assertNotEquals("", id);
            assertEquals(2, outputLogs1.size());
            assertEquals(1, outputLogs2.size());
            assertEquals(1, outputLogs3.size());

            /* Delete. */
            persistence.deleteLogs("", id);

            /* Create a query builder for column group. */
            SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
            builder.appendWhere(DatabasePersistence.COLUMN_GROUP + " = ?");

            /* Access DatabaseStorage directly to verify the deletions. */
            Cursor cursor1 = persistence.mDatabaseManager.getCursor(builder, null, new String[]{"test-p1"}, null);
            Cursor cursor2 = persistence.mDatabaseManager.getCursor(builder, null, new String[]{"test-p2"}, null);
            Cursor cursor3 = persistence.mDatabaseManager.getCursor(builder, null, new String[]{"test-p3"}, null);

            //noinspection TryFinallyCanBeTryWithResources
            try {

                /* Verify. */
                assertEquals(2, cursor1.getCount());
                assertEquals(1, cursor2.getCount());
                assertEquals(1, cursor3.getCount());
            } finally {

                /* Close. */
                cursor1.close();
                cursor2.close();
                cursor3.close();
            }

            /* Delete. */
            persistence.deleteLogs("test-p1", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            Cursor cursor4 = persistence.mDatabaseManager.getCursor(builder, null, new String[]{"test-p1"}, null);

            //noinspection TryFinallyCanBeTryWithResources
            try {

                /* Verify. */
                assertEquals(0, cursor4.getCount());
            } finally {

                /* Close. */
                cursor4.close();
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
            persistence.putLog(log1, "test-p1", NORMAL);
            persistence.putLog(log2, "test-p1", NORMAL);
            persistence.putLog(log3, "test-p2", NORMAL);
            persistence.putLog(log4, "test-p3", NORMAL);

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            String id1 = persistence.getLogs("test-p1", Collections.<String>emptyList(), 5, outputLogs, null, null);
            String id2 = persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs, null, null);
            assertNotNull(id1);
            assertNotNull(id2);

            /* Delete. */
            persistence.deleteLogs("test-p1");
            persistence.deleteLogs("test-p3");

            /* Try another get for verification. */
            outputLogs.clear();
            persistence.getLogs("test-p3", Collections.<String>emptyList(), 5, outputLogs, null, null);

            /* Verify. */
            Map<String, List<Long>> pendingGroups = persistence.mPendingDbIdentifiersGroups;
            assertNull(pendingGroups.get("test-p1" + id1));
            List<Long> p2Logs = pendingGroups.get("test-p2" + id2);
            assertNotNull(p2Logs);
            assertEquals(1, p2Logs.size());
            assertEquals(1, pendingGroups.size());
            assertEquals(0, outputLogs.size());
            assertEquals(1, persistence.mDatabaseManager.getRowCount());

            /* Verify one log still persists in the database. */
            persistence.clearPendingLogState();
            outputLogs.clear();
            persistence.getLogs("test-p2", Collections.<String>emptyList(), 5, outputLogs, null, null);
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
    public void getLogsWithNormalPriority() throws PersistenceException {

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
                persistence.putLog(logs[i], "test", NORMAL);
            }

            /* Get. */
            getAllLogs(persistence, numberOfLogs, sizeForGetLogs);

            /* Clear ids, we should be able to get the logs again in the same sequence. */
            persistence.clearPendingLogState();
            getAllLogs(persistence, numberOfLogs, sizeForGetLogs);

            /* Count. */
            assertEquals(10, persistence.countLogs("test"));

            /* Clear. Nothing to get after. */
            persistence.mDatabaseManager.clear();
            List<Log> outputLogs = new ArrayList<>();
            assertNull(persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs, null, null));
            assertTrue(outputLogs.isEmpty());
            assertEquals(0, persistence.countLogs("test"));
        } finally {
            persistence.close();
        }
    }

    private void getAllLogs(DatabasePersistence persistence, int numberOfLogs, int sizeForGetLogs) {
        List<Log> outputLogs = new ArrayList<>();
        int expected = 0;
        do {
            numberOfLogs -= expected;
            persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs, null, null);
            expected = Math.min(Math.max(numberOfLogs, 0), sizeForGetLogs);
            assertEquals(expected, outputLogs.size());
            outputLogs.clear();
        } while (numberOfLogs > 0);

        /* Get should be 0 now. */
        persistence.getLogs("test", Collections.<String>emptyList(), sizeForGetLogs, outputLogs, null, null);
        assertEquals(0, outputLogs.size());
    }

    @Test
    public void getLogsWithMixedPriorities() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        try {

            /* Put a normal log. */
            Log log1 = AndroidTestUtils.generateMockLog();
            persistence.putLog(log1, "test", NORMAL);

            /* Put a critical log. */
            Log log2 = AndroidTestUtils.generateMockLog();
            persistence.putLog(log2, "test", CRITICAL);

            /* Put a normal log again. */
            Log log3 = AndroidTestUtils.generateMockLog();
            persistence.putLog(log3, "test", NORMAL);

            /* Put a critical log again. */
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog(log4, "test", CRITICAL);

            /* Expected order. */
            List<Log> expectedLogs = new ArrayList<>();
            expectedLogs.add(log2);
            expectedLogs.add(log4);
            expectedLogs.add(log1);
            expectedLogs.add(log3);

            /* Get logs and check order. */
            List<Log> actualLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), expectedLogs.size(), actualLogs, null, null);
            assertEquals(expectedLogs, actualLogs);
        } finally {
            persistence.close();
        }
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
            String batchId = persistence.getLogs("test", Arrays.asList(pausedKey1, pausedKey2), limit, outLogs, null, null);
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
            batchId = persistence.getLogs("test", Arrays.asList(pausedKey1, pausedKey2), limit, outLogs, null, null);
            assertNull(batchId);
            assertEquals(0, outLogs.size());

            /* If we try to get a second batch without filtering, we should get all disabled logs. */
            outLogs.clear();
            batchId = persistence.getLogs("test", Collections.<String>emptyList(), limit, outLogs, null, null);
            assertNotNull(batchId);
            assertEquals(numberOfLogsPerKey * 2, outLogs.size());
            for (Log log : outLogs) {
                assertTrue(log instanceof CommonSchemaLog);
                assertNotEquals(resumedKey, ((CommonSchemaLog) log).getIKey());
            }
        } finally {
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
            persistence.putLog(log, "test", NORMAL);
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
                persistence.putLog(log, "test", NORMAL);

            /* Get. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 10, outputLogs, null, null);
            assertEquals(numberOfLogs / 2, outputLogs.size());
            assertEquals(2, persistence.mDatabaseManager.getRowCount());
        } finally {
            persistence.close();
        }
    }

    @Test
    public void getLogsWithNullDate() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Get logs. */
        List<Log> outputLogs = new ArrayList<>();
        persistence.getLogs("test", Collections.<String>emptyList(), 4, outputLogs, null, null);
        assertEquals(4, outputLogs.size());
    }

    @Test
    public void getLogsWithYesterdayDate() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Get logs without disabled keys. */
        List<Log> outputLogs = new ArrayList<>();

        /* Create yesterday date. */
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date to = cal.getTime();

        persistence.getLogs("test", Collections.<String>emptyList(), 4, outputLogs, null, to);
        assertEquals(3, outputLogs.size());
    }

    @Test
    public void getLogsWithTomorrowDate() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Get logs and check order. */
        List<Log> outputLogs = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date to = cal.getTime();

        persistence.getLogs("test", Collections.<String>emptyList(), 4, outputLogs, null, to);
        assertEquals(4, outputLogs.size());
    }

    @Test
    public void getLogsFromYesterday() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Get logs and check order. */
        List<Log> outputLogs = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date from = cal.getTime();
        persistence.getLogs("test", Collections.<String>emptyList(), 4, outputLogs, from, null);

        /* Verify. */
        assertEquals(1, outputLogs.size());
    }

    @Test
    public void getLogsForPeriod() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Get logs and check order. */
        List<Log> outputLogs = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date to = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date from = cal.getTime();
        persistence.getLogs("test", Collections.<String>emptyList(), 4, outputLogs, from, to);

        /* Verify. */
        assertEquals(2, outputLogs.size());
    }

    @Test
    public void countLogsWithYesterdayDate() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Create yesterday date. */
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date to = cal.getTime();

        /* Count logs. */
        int actualCount = persistence.countLogs(to);
        assertEquals(3, actualCount);
    }

    @Test
    public void countLogsWithTomorrowDate() throws PersistenceException {

        /* Initialize database persistence. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);
        buildLogs(persistence);

        /* Create yesterday date. */
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date to = cal.getTime();

        /* Count logs. */
        int actualCount = persistence.countLogs(to);
        assertEquals(4, actualCount);
    }

    private void buildLogs(DatabasePersistence persistence) throws PersistenceException {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());

            /* Put a log with current date. */
            Log log1 = AndroidTestUtils.generateMockLog();
            log1.setTimestamp(calendar.getTime());
            persistence.putLog(log1, "test", NORMAL);

            /* Put a log with yesterday date. */
            calendar.add(Calendar.DATE, -1);
            Log log2 = AndroidTestUtils.generateMockLog();
            log2.setTimestamp(calendar.getTime());
            persistence.putLog(log2, "test", NORMAL);

            /* Put a log with 2 days ago date. */
            calendar.add(Calendar.DATE, -1);
            Log log3 = AndroidTestUtils.generateMockLog();
            log3.setTimestamp(calendar.getTime());
            persistence.putLog(log3, "test", NORMAL);

            /* Put a log with 3 days ago date. */
            calendar.add(Calendar.DATE, -1);
            Log log4 = AndroidTestUtils.generateMockLog();
            log4.setTimestamp(calendar.getTime());
            persistence.putLog(log4, "test", NORMAL);
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion1to5() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_TOKEN);
        oldSchema.remove(DatabasePersistence.COLUMN_DATA_TYPE);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_KEY);
        oldSchema.remove(DatabasePersistence.COLUMN_PRIORITY);
        oldSchema.remove(DatabasePersistence.COLUMN_TIMESTAMP);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DatabasePersistence.DATABASE, DatabasePersistence.TABLE, 1, oldSchema, mock(DatabaseManager.Listener.class));

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
            databaseManager.put(contentValues, DatabasePersistence.COLUMN_PRIORITY);
        } finally {
            databaseManager.close();
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
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Check priority migration. */
            ContentValues values = getContentValues(persistence, "test");
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Put new data with token. */
            persistence.putLog(commonSchemaLog, "test/one", NORMAL);
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
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = getContentValues(persistence, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);

            /* Verify priority stored too. */
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion2to5() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_TARGET_KEY);
        oldSchema.remove(DatabasePersistence.COLUMN_PRIORITY);
        oldSchema.remove(DatabasePersistence.COLUMN_TIMESTAMP);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DatabasePersistence.DATABASE, DatabasePersistence.TABLE, DatabasePersistence.VERSION_TYPE_API_KEY, oldSchema, mock(DatabaseManager.Listener.class));

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
            databaseManager.put(contentValues, DatabasePersistence.COLUMN_PRIORITY);
        } finally {
            databaseManager.close();
        }

        /* Upgrade. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);

        /* Prepare a common schema log. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        commonSchemaLog.setName("test");
        commonSchemaLog.setIKey("o:test");
        commonSchemaLog.setTimestamp(new Date());
        Long timestamp = commonSchemaLog.getTimestamp().getTime();
        commonSchemaLog.setVer("3.0");
        commonSchemaLog.addTransmissionTarget("test-guid");

        /* Check upgrade. */
        try {

            /* Get old data. */
            assertEquals(1, persistence.countLogs("test"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Check priority migration. */
            ContentValues values = getContentValues(persistence, "test");
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Put new data with token. */
            persistence.putLog(commonSchemaLog, "test/one", NORMAL);
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
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = getContentValues(persistence, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);

            /* Verify priority stored too. */
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Verify timestamp. */
            assertEquals(timestamp, values.getAsLong(DatabasePersistence.COLUMN_TIMESTAMP));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion3to5() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_PRIORITY);
        oldSchema.remove(DatabasePersistence.COLUMN_TIMESTAMP);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DatabasePersistence.DATABASE, DatabasePersistence.TABLE, DatabasePersistence.VERSION_TARGET_KEY, oldSchema, mock(DatabaseManager.Listener.class));

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
            databaseManager.put(contentValues, DatabasePersistence.COLUMN_PRIORITY);
        } finally {
            databaseManager.close();
        }

        /* Upgrade. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);

        /* Prepare a common schema log. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        commonSchemaLog.setName("test");
        commonSchemaLog.setIKey("o:test");
        commonSchemaLog.setTimestamp(new Date());
        Long timestamp = commonSchemaLog.getTimestamp().getTime();
        commonSchemaLog.setVer("3.0");
        commonSchemaLog.addTransmissionTarget("test-guid");

        /* Check upgrade. */
        try {

            /* Get old data. */
            assertEquals(1, persistence.countLogs("test"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Check priority migration. */
            ContentValues values = getContentValues(persistence, "test");
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Put new data with token. */
            persistence.putLog(commonSchemaLog, "test/one", CRITICAL);
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
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = getContentValues(persistence, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);

            /* Verify priority stored too. */
            assertEquals((Integer) CRITICAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Verify timestamp. */
            assertEquals(timestamp, values.getAsLong(DatabasePersistence.COLUMN_TIMESTAMP));
        } finally {
            persistence.close();
        }
    }

    @Test
    public void upgradeFromVersion4to5() throws PersistenceException, JSONException {

        /* Initialize database persistence with old schema. */
        ContentValues oldSchema = new ContentValues(SCHEMA);
        oldSchema.remove(DatabasePersistence.COLUMN_TIMESTAMP);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DatabasePersistence.DATABASE, DatabasePersistence.TABLE, DatabasePersistence.VERSION_PRIORITY_KEY, oldSchema, mock(DatabaseManager.Listener.class));

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
            contentValues.put(DatabasePersistence.COLUMN_PRIORITY, NORMAL);
            databaseManager.put(contentValues, DatabasePersistence.COLUMN_PRIORITY);
        } finally {
            databaseManager.close();
        }

        /* Upgrade. */
        DatabasePersistence persistence = new DatabasePersistence(sContext);
        persistence.setLogSerializer(logSerializer);

        /* Prepare a common schema log. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        commonSchemaLog.setName("test");
        commonSchemaLog.setIKey("o:test");
        commonSchemaLog.setTimestamp(new Date());
        Long timestamp = commonSchemaLog.getTimestamp().getTime();
        commonSchemaLog.setVer("3.0");
        commonSchemaLog.addTransmissionTarget("test-guid");

        /* Check upgrade. */
        try {

            /* Get old data. */
            assertEquals(1, persistence.countLogs("test"));
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(oldLog, outputLogs.get(0));

            /* Check priority migration. */
            ContentValues values = getContentValues(persistence, "test");
            assertEquals((Integer) NORMAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Put new data with token. */
            persistence.putLog(commonSchemaLog, "test/one", CRITICAL);
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
            persistence.getLogs("test/one", Collections.<String>emptyList(), 1, outputLogs, null, null);
            assertEquals(1, outputLogs.size());
            assertEquals(commonSchemaLog, outputLogs.get(0));

            /* Verify target token is encrypted. */
            ContentValues values = getContentValues(persistence, "test/one");
            String token = values.getAsString(DatabasePersistence.COLUMN_TARGET_TOKEN);
            assertNotNull(token);
            assertNotEquals("test-guid", token);
            assertEquals("test-guid", CryptoUtils.getInstance(sContext).decrypt(token, false).getDecryptedData());

            /* Verify target key stored as well. */
            String targetKey = values.getAsString(DatabasePersistence.COLUMN_TARGET_KEY);
            assertEquals(commonSchemaLog.getIKey(), "o:" + targetKey);

            /* Verify priority stored too. */
            assertEquals((Integer) CRITICAL, values.getAsInteger(DatabasePersistence.COLUMN_PRIORITY));

            /* Verify timestamp. */
            assertEquals(timestamp, values.getAsLong(DatabasePersistence.COLUMN_TIMESTAMP));
        } finally {
            persistence.close();
        }
    }
}
