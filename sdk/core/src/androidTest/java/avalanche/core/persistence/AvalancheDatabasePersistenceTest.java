package avalanche.core.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.ingestion.models.json.MockLog;
import avalanche.core.ingestion.models.json.MockLogFactory;
import avalanche.core.persistence.AvalanchePersistence.PersistenceException;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.StorageHelper.DatabaseStorage.DatabaseScanner;
import avalanche.core.utils.UUIDUtils;

import static avalanche.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AvalancheDatabasePersistenceTest {

    /**
     * Log tag.
     */
    private static final String TAG = "DatabasePersistenceTest";

    /**
     * Context instance.
     */
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    @AfterClass
    public static void tearDownClass() {
        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    private static Log generateLog() {
        Random random = new Random(System.currentTimeMillis());

        Device device = new Device();
        device.setSdkVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setModel("S5");
        device.setOemName("HTC");
        device.setOsName("Android");
        device.setOsVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setOsApiLevel(random.nextInt(9) + 15);
        device.setLocale("en_US");
        device.setTimeZoneOffset(random.nextInt(52) * 30 - 720);
        device.setScreenSize(String.format(Locale.ENGLISH, "%dx%d", (random.nextInt(4) + 1) * 1000, (random.nextInt(10) + 1) * 100));
        device.setAppVersion(String.format(Locale.ENGLISH, "%d.%d.%d", (random.nextInt(5) + 1), random.nextInt(10), random.nextInt(100)));
        device.setAppBuild(Integer.toString(random.nextInt(1000) + 1));
        device.setAppNamespace("com.microsoft.unittest");

        Log log = new MockLog();
        log.setSid(UUIDUtils.randomUUID());
        log.setDevice(device);
        return log;
    }

    private static int getIteratorSize(Iterator iterator) {
        int count = 0;
        for (; iterator.hasNext(); iterator.next())
            count++;
        return count;
    }

    @Test
    public void putLog() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putLog", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log = generateLog();
            persistence.putLog("test-p1", log);

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void putTooManyLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog with too many logs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putTooManyLogs", 1, 2);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate too many logs and persist. */
            Log log1 = generateLog();
            Log log2 = generateLog();
            Log log3 = generateLog();
            Log log4 = generateLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p1", log3);
            persistence.putLog("test-p1", log4);

            /* Get logs from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", 4, outputLogs);
            assertEquals(2, outputLogs.size());
            assertEquals(log3, outputLogs.get(0));
            assertEquals(log4, outputLogs.get(1));
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test(expected = PersistenceException.class)
    public void putLogException() throws PersistenceException, IOException, JSONException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog with serialize exception");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putLogException", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        doThrow(new JSONException("JSON exception")).when(logSerializer).serializeLog(any(Log.class));
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log = generateLog();
            persistence.putLog("test-p1", log);
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void deleteLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence deleteLogs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "deleteLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log1 = generateLog();
            Log log2 = generateLog();
            Log log3 = generateLog();
            Log log4 = generateLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p2", log3);
            persistence.putLog("test-p3", log4);

            /* Get a log from persistence. */
            List<Log> outputLogs1 = new ArrayList<>();
            List<Log> outputLogs2 = new ArrayList<>();
            List<Log> outputLogs3 = new ArrayList<>();
            String id = persistence.getLogs("test-p1", 5, outputLogs1);
            persistence.getLogs("test-p2", 5, outputLogs2);
            persistence.getLogs("test-p3", 5, outputLogs3);

            /* Verify. */
            assertNotNull(id);
            assertNotEquals("", id);
            assertEquals(2, outputLogs1.size());
            assertEquals(1, outputLogs2.size());
            assertEquals(1, outputLogs3.size());

            /* Delete. */
            persistence.deleteLog("", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner1 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_KEY, "test-p1");
            DatabaseScanner scanner2 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_KEY, "test-p2");
            DatabaseScanner scanner3 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_KEY, "test-p3");

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
            persistence.deleteLog("test-p1", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner4 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_KEY, "test-p1");

            //noinspection TryFinallyCanBeTryWithResources
            try {
                /* Verify. */
                assertEquals(0, getIteratorSize(scanner4.iterator()));
            } finally {
                /* Close. */
                scanner4.close();
            }

        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void getLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence getLogs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "getLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Test constants. */
            int numberOfLogs = 10;
            int sizeForGetLogs = 4;

            /* Generate a log and persist. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++)
                logs[i] = generateLog();

            /* Put. */
            for (Log log : logs)
                persistence.putLog("test", log);

            /* Get. */
            getAllLogs(persistence, "test", numberOfLogs, sizeForGetLogs);

            /* Clear ids, we should be able to get the logs again in the same sequence. */
            persistence.clearPendingLogState();
            getAllLogs(persistence, "test", numberOfLogs, sizeForGetLogs);

            /* Clear. Nothing to get after. */
            persistence.clear();
            List<Log> outputLogs = new ArrayList<>();
            assertNull(persistence.getLogs("test", sizeForGetLogs, outputLogs));
            assertTrue(outputLogs.isEmpty());
        } finally {

            /* Close. */
            persistence.close();
        }
    }

    private void getAllLogs(AvalancheDatabasePersistence persistence, String key, int numberOfLogs, int sizeForGetLogs) {
        List<Log> outputLogs = new ArrayList<>();
        int expected = 0;
        do {
            numberOfLogs -= expected;
            persistence.getLogs(key, sizeForGetLogs, outputLogs);
            expected = Math.min(Math.max(numberOfLogs, 0), sizeForGetLogs);
            assertEquals(expected, outputLogs.size());
            outputLogs.clear();
        } while (numberOfLogs > 0);

        /* Get should be 0 now. */
        persistence.getLogs(key, sizeForGetLogs, outputLogs);
        assertEquals(0, outputLogs.size());
    }

    @Test
    public void getLogsException() throws PersistenceException, IOException, JSONException {
        android.util.Log.i(TAG, "Testing Database Persistence getLogs with deserialize exception");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "getLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = spy(new DefaultLogSerializer());
        /* Throw a JSON exception for the first call. */
        doThrow(new JSONException("JSON exception"))
                /* Return a normal log for the second call. */
                .doReturn(generateLog())
                /* Throw a JSON exception for the third call. */
                .doThrow(new JSONException("JSON exception"))
                /* Return a normal log for further calls. */
                .doReturn(generateLog())
                .when(logSerializer).deserializeLog(anyString());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Test constants. */
            int numberOfLogs = 4;

            /* Generate a log and persist. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++)
                logs[i] = generateLog();

            /* Put. */
            for (Log log : logs)
                persistence.putLog("test", log);

            /* Get. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", 10, outputLogs);
            assertEquals(numberOfLogs / 2, outputLogs.size());
            assertEquals(2, persistence.mDatabaseStorage.size());
        } finally {
            /* Close. */
            persistence.close();
        }
    }
}
