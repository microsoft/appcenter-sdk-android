package avalanche.core.utils;

import android.content.ContentValues;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static avalanche.core.utils.StorageHelper.DatabaseStorage;
import static avalanche.core.utils.StorageHelper.InternalStorage;
import static avalanche.core.utils.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class StorageHelperAndroidTest {
    /**
     * Log tag.
     */
    private static final String TAG = "StorageHelperTest";

    /**
     * File extension.
     */
    private static final String INTERNAL_STORAGE_TEST_FILE_EXTENSION = ".stacktrace";

    /**
     * Context instance.
     */
    private static Context sContext;

    /**
     * Root path for private files.
     */
    private static String sAndroidFilesPath;

    /**
     * Database schema.
     */
    private static ContentValues mSchema;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
        sAndroidFilesPath = sContext.getFilesDir().getAbsolutePath() + "/test";

        /* Create a test directory. */
        InternalStorage.mkdir(sAndroidFilesPath);

        /* Create a test schema. */
        mSchema = generateContentValues();
    }

    @AfterClass
    public static void tearDownClass() {
        /* Clean up shared preferences. */
        try {
            for (SharedPreferencesTestData data : generateSharedPreferenceData()) {
                String key = data.value.getClass().getCanonicalName();
                PreferencesStorage.remove(key);
            }
        } catch (NoSuchMethodException ignored) {
            /* Ignore exception. */
        }

        /* Clean up internal storage. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(INTERNAL_STORAGE_TEST_FILE_EXTENSION);
            }
        };

        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }

        InternalStorage.delete(sAndroidFilesPath);

        /* Delete database. */
        sContext.deleteDatabase("test-databaseStorage");
        sContext.deleteDatabase("test-databaseStorageUpgrade");
        sContext.deleteDatabase("test-putTooManyLogs");
        sContext.deleteDatabase("test-databaseStorageScannerRemove");
        sContext.deleteDatabase("test-databaseStorageScannerNext");
        sContext.deleteDatabase("test-databaseStorageInMemoryDB");
    }

    private static SharedPreferencesTestData[] generateSharedPreferenceData() throws NoSuchMethodException {
        SharedPreferencesTestData[] testData = new SharedPreferencesTestData[6];

        /* boolean */
        testData[0] = new SharedPreferencesTestData();
        testData[0].value = true;
        testData[0].defaultValue = false;
        testData[0].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getBoolean", String.class);
        testData[0].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getBoolean", String.class, boolean.class);
        testData[0].putMethod = PreferencesStorage.class.getDeclaredMethod("putBoolean", String.class, boolean.class);

        /* float */
        testData[1] = new SharedPreferencesTestData();
        testData[1].value = 111.22f;
        testData[1].defaultValue = 0.01f;
        testData[1].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getFloat", String.class);
        testData[1].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getFloat", String.class, float.class);
        testData[1].putMethod = PreferencesStorage.class.getDeclaredMethod("putFloat", String.class, float.class);

        /* int */
        testData[2] = new SharedPreferencesTestData();
        testData[2].value = 123;
        testData[2].defaultValue = -1;
        testData[2].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getInt", String.class);
        testData[2].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getInt", String.class, int.class);
        testData[2].putMethod = PreferencesStorage.class.getDeclaredMethod("putInt", String.class, int.class);

        /* long */
        testData[3] = new SharedPreferencesTestData();
        testData[3].value = 123456789000L;
        testData[3].defaultValue = 345L;
        testData[3].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getLong", String.class);
        testData[3].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getLong", String.class, long.class);
        testData[3].putMethod = PreferencesStorage.class.getDeclaredMethod("putLong", String.class, long.class);

        /* String */
        testData[4] = new SharedPreferencesTestData();
        testData[4].value = "Hello World";
        testData[4].defaultValue = "Empty";
        testData[4].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getString", String.class);
        testData[4].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getString", String.class, String.class);
        testData[4].putMethod = PreferencesStorage.class.getDeclaredMethod("putString", String.class, String.class);

        /* Set<String> */
        Set<String> data = new HashSet<>();
        data.add("ABC");
        data.add("Hello World");
        data.add("Welcome to the world!");
        Set<String> defaultSet = new HashSet<>();
        defaultSet.add("DEFAULT");

        testData[5] = new SharedPreferencesTestData();
        testData[5].value = data;
        testData[5].defaultValue = defaultSet;
        testData[5].getMethod1 = PreferencesStorage.class.getDeclaredMethod("getStringSet", String.class);
        testData[5].getMethod2 = PreferencesStorage.class.getDeclaredMethod("getStringSet", String.class, Set.class);
        testData[5].putMethod = PreferencesStorage.class.getDeclaredMethod("putStringSet", String.class, Set.class);

        return testData;
    }

    private static ContentValues generateContentValues() {
        Random random = new Random(System.currentTimeMillis());
        byte[] randomBytes = new byte[10];
        random.nextBytes(randomBytes);

        ContentValues values = new ContentValues();
        values.put("COL_STRING", new String(randomBytes));
        values.put("COL_BYTE", randomBytes[0]);
        values.put("COL_SHORT", (short) random.nextInt(100));
        values.put("COL_INTEGER", random.nextInt());
        values.put("COL_LONG", random.nextLong());
        values.put("COL_FLOAT", random.nextFloat());
        values.put("COL_DOUBLE", random.nextDouble());
        values.put("COL_BOOLEAN", Boolean.TRUE/*random.nextBoolean()*/);
        values.put("COL_BYTE_ARRAY", randomBytes);
        return values;
    }

    public static void assertContentValuesEquals(ContentValues expected, ContentValues actual) {
        assertEquals(expected.getAsString("COL_STRING"), actual.getAsString("COL_STRING"));
        assertEquals(expected.getAsByte("COL_BYTE"), actual.getAsByte("COL_BYTE"));
        assertEquals(expected.getAsShort("COL_SHORT"), actual.getAsShort("COL_SHORT"));
        assertEquals(expected.getAsInteger("COL_INTEGER"), actual.getAsInteger("COL_INTEGER"));
        assertEquals(expected.getAsLong("COL_LONG"), actual.getAsLong("COL_LONG"));
        assertEquals(expected.getAsFloat("COL_FLOAT"), actual.getAsFloat("COL_FLOAT"));
        assertEquals(expected.getAsDouble("COL_DOUBLE"), actual.getAsDouble("COL_DOUBLE"));
        assertEquals(expected.getAsBoolean("COL_BOOLEAN"), actual.getAsBoolean("COL_BOOLEAN"));
        assertArrayEquals(expected.getAsByteArray("COL_BYTE_ARRAY"), actual.getAsByteArray("COL_BYTE_ARRAY"));
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void runDatabaseStorageTest(DatabaseStorage databaseStorage, boolean imdbTest) {
        ContentValues firstValue = generateContentValues();
        ContentValues secondValue = generateContentValues();
        ContentValues thirdValue = generateContentValues();

        /* Put. */
        Long firstValueId = databaseStorage.put(firstValue);
        assertNotNull(firstValueId);


        /* Get for in-memory database test. */
        if (imdbTest) {
            ContentValues firstValueFromDatabase = databaseStorage.get("COL_STRING", firstValue.getAsString("COL_STRING"));
            assertContentValuesEquals(firstValue, firstValueFromDatabase);
            firstValueFromDatabase = databaseStorage.get("COL_STRING", firstValue.getAsString("COL_STRING") + "X");
            assertNull(firstValueFromDatabase);
        }

        /* Put another. */
        Long secondValueId = databaseStorage.put(secondValue);
        assertNotNull(secondValueId);

        /* Generate an ID that is neither firstValueId nor secondValueId. */

        /* Get. */
        ContentValues firstValueFromDatabase = databaseStorage.get(firstValueId);
        assertContentValuesEquals(firstValue, firstValueFromDatabase);
        ContentValues secondValueFromDatabase = databaseStorage.get(DatabaseManager.PRIMARY_KEY, secondValueId);
        assertContentValuesEquals(secondValue, secondValueFromDatabase);
        @SuppressWarnings("ResourceType")
        ContentValues nullValueFromDatabase = databaseStorage.get(-1);
        assertNull(nullValueFromDatabase);

        /* Update. */
        assertTrue(databaseStorage.update(firstValueId, thirdValue));
        ContentValues thirdValueFromDatabase = databaseStorage.get(firstValueId);
        assertContentValuesEquals(thirdValue, thirdValueFromDatabase);

        /* Delete. */
        databaseStorage.delete(firstValueId);
        assertNull(databaseStorage.get(firstValueId));
        assertEquals(1, databaseStorage.getRowCount());

        /* Clear. */
        databaseStorage.clear();
        assertEquals(0, databaseStorage.getRowCount());
    }

    /**
     * This method is to go through the code that intentionally ignores exceptions.
     */
    @Test
    public void storageHelperForCoverage() throws IOException {
        Log.i(TAG, "Testing StorageHelper for code coverage");

        new StorageHelper();
        new PreferencesStorage();
        new InternalStorage();
    }

    @Test
    public void sharedPreferences() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Log.i(TAG, "Testing Shared Preference");
        for (SharedPreferencesTestData data : generateSharedPreferenceData()) {
            Log.i(TAG, "Get/Put test for type " + data.value.getClass().getSimpleName());

            /* Put value to shared preferences. */
            String key = data.value.getClass().getCanonicalName();
            data.putMethod.invoke(null, key, data.value);

            /* Get value from shared preferences. */
            Object actual = data.getMethod1.invoke(null, key);

            /* Verify the value is same as assigned. */
            assertEquals(data.value, actual);

            /* Remove key from shared preferences. */
            PreferencesStorage.remove(key);

            /* Verify the value equals to default value. */
            assertEquals(data.defaultValue, data.getMethod2.invoke(null, key, data.defaultValue));
        }
    }

    @Test
    public void internalStorage() throws IOException, InterruptedException {
        Log.i(TAG, "Testing Internal Storage file read/write");

        final String prefix = Long.toString(System.currentTimeMillis());

        /* Create a mock data. */
        String filename1 = prefix + "-" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION;
        String contents1 = "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.isEmpty()' on a null object reference\n" +
                "at avalanche.base.utils.StorageHelperAndroidTest.internalStorage(StorageHelperAndroidTest.java:124)\n" +
                "at java.lang.reflect.Method.invoke(Native Method)\n" +
                "at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)";
        String filename2 = prefix + "-" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION;
        //noinspection SpellCheckingInspection
        String contents2 = "java.io.FileNotFoundException: 6c1b1c58-1c2f-47d9-8f04-52639c3a804d: open failed: EROFS (Read-only file system)\n" +
                "at libcore.io.IoBridge.open(IoBridge.java:452)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:87)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:72)\n" +
                "at java.io.FileWriter.<init>(FileWriter.java:42)";
        String filename3 = prefix + "-" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION;

        /* FilenameFilter to look up files that are created in current test. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix) && filename.endsWith(INTERNAL_STORAGE_TEST_FILE_EXTENSION);
            }
        };

        /* Write contents to test files after 2 sec delay. */
        Log.i(TAG, "Writing " + filename1);
        InternalStorage.write(sAndroidFilesPath + "/" + filename1, contents1);
        TimeUnit.SECONDS.sleep(2);
        Log.i(TAG, "Writing " + filename2);
        InternalStorage.write(sAndroidFilesPath + "/" + filename2, contents2);
        /* Also write empty content to a test file. */
        TimeUnit.SECONDS.sleep(2);
        InternalStorage.write(sAndroidFilesPath + "/" + filename3, "");

        /* Get file names in the root path. */
        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Verify the files are created. */
        assertNotNull(filenames);
        assertEquals(2, filenames.length);
        List<String> list = Arrays.asList(filenames);
        assertTrue(list.contains(filename1));
        assertTrue(list.contains(filename2));
        assertFalse(list.contains(filename3));

        /* Get the most recent file. */
        File lastModifiedFile = InternalStorage.lastModifiedFile(sAndroidFilesPath, filter);

        /* Verify the most recent file. */
        assertNotNull(lastModifiedFile);
        assertEquals(filename2, lastModifiedFile.getName());

        /* Read the most recent file. */
        String actual = InternalStorage.read(lastModifiedFile);

        /* Verify the contents of the most recent file. */
        assertEquals(contents2, actual.trim());

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            Log.i(TAG, "Deleting " + filename);
            InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }

        /* Verify all the files are properly deleted. */
        assertEquals(0, InternalStorage.getFilenames(sAndroidFilesPath, filter).length);

        /* Verify invalid accesses. */
        assertEquals("", InternalStorage.read("not-exist-filename"));
        assertArrayEquals(new String[0], InternalStorage.getFilenames("not-exist-path", null));
        assertNull(InternalStorage.lastModifiedFile("not-exist-path", null));
    }

    @Test
    public void internalStorageForObject() throws IOException, ClassNotFoundException {
        Log.i(TAG, "Testing Internal Storage object serialization");

        File file = new File(sAndroidFilesPath + "/" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION);

        /* Create a mock object. */
        DataModel model = new DataModel(10, "Model", true);

        /* Write the object to a file. */
        InternalStorage.writeObject(file, model);

        /* Read the file. */
        DataModel actual = InternalStorage.readObject(file);

        /* Verify the de-serialized instance and original instance are same. */
        assertNotNull(actual);
        assertEquals(model.number, actual.number);
        assertEquals(model.object.text, actual.object.text);
        assertEquals(model.object.enabled, actual.object.enabled);

        /* Delete the files to clean up. */
        Log.i(TAG, "Deleting " + file.getName());
        InternalStorage.delete(file);
    }

    @Test
    public void databaseStorage() throws IOException {
        Log.i(TAG, "Testing Database Storage");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorage", "databaseStorage", 1, mSchema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            runDatabaseStorageTest(databaseStorage, false);
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    @Test
    public void databaseStorageUpgrade() throws IOException {
        Log.i(TAG, "Testing Database Storage Upgrade");

        /* Create a schema for v1. */
        ContentValues schema = new ContentValues();
        schema.put("COL_STRING", "");

        /* Create a row for v1. */
        ContentValues oldVersionValue = new ContentValues();
        oldVersionValue.put("COL_STRING", "Hello World");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 1, schema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        try {
            /* Database will always create a column for identifiers so default length of all tables is 1. */
            assertEquals(2, databaseStorage.getColumnNames().length);
        } finally {
            /* Close. */
            databaseStorage.close();
        }

        /* Get instance to access database with a newer schema. */
        databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 2, mSchema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        try {
            assertEquals(10, databaseStorage.getColumnNames().length);
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    @Test
    public void putTooManyLogs() throws IOException {
        Log.i(TAG, "Testing Database Storage Capacity");

        /* Get instance to access database. */
        final int capacity = 2;
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-putTooManyLogs", "putTooManyLogs", 1, mSchema, capacity, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            ContentValues firstValue = generateContentValues();
            ContentValues secondValue = generateContentValues();
            ContentValues thirdValue = generateContentValues();

            /* Put. */
            Long firstValueId = databaseStorage.put(firstValue);
            Long secondValueId = databaseStorage.put(secondValue);
            Long thirdValueId = databaseStorage.put(thirdValue);

            assertNotNull(firstValueId);
            assertNotNull(secondValueId);
            assertNotNull(thirdValueId);

            assertEquals(capacity, databaseStorage.getRowCount());
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void databaseStorageScannerRemove() throws IOException {
        Log.i(TAG, "Testing Database Storage Exceptions");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageScannerRemove", "databaseStorageScannerRemove", 1, mSchema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            databaseStorage.getScanner().iterator().remove();
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void databaseStorageScannerNext() throws IOException {
        Log.i(TAG, "Testing Database Storage Exceptions");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageScannerNext", "databaseStorageScannerNext", 1, mSchema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            databaseStorage.getScanner().iterator().next();
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    /* This is a hack to test database failure by passing a weird table name which is actually valid.
       SQLite database allows to create a table that contains period (.) but it doesn't actually create the table and doesn't raise any exceptions.
       This test method will then be able to test in-memory database by accessing a table which is not created.
       Only tested on emulator so it might not work in the future or on any other devices. */
    @Test
    public void databaseStorageInMemoryDB() throws IOException {
        Log.i(TAG, "Testing Database Storage switch over to in-memory database");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageInMemoryDB", "test.databaseStorageInMemoryDB", 1, mSchema, new DatabaseStorage.DatabaseErrorListener() {
            @Override
            public void onError(String operation, RuntimeException e) {
                /* Do not handle any errors. This is simulating errors so this is expected. */
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            runDatabaseStorageTest(databaseStorage, true);
        } finally {
            /* Close. */
            databaseStorage.close();
        }
    }

    /**
     * Temporary class for testing shared preferences.
     */
    private static class SharedPreferencesTestData {
        Object value;
        Object defaultValue;
        Method getMethod1;
        Method getMethod2;
        Method putMethod;
    }

    /**
     * Temporary class for testing object serialization.
     */
    private static class DataModel implements Serializable {
        final int number;
        final InnerModel object;

        DataModel(int number, String text, boolean enabled) {
            this.number = number;
            this.object = new InnerModel(text, enabled);
        }

        static class InnerModel implements Serializable {
            final String text;
            final boolean enabled;

            InnerModel(String text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }
    }
}