package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.microsoft.appcenter.utils.UUIDUtils;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.utils.storage.DatabaseManager.ALLOWED_SIZE_MULTIPLE;
import static com.microsoft.appcenter.utils.storage.StorageHelper.DatabaseStorage;
import static com.microsoft.appcenter.utils.storage.StorageHelper.InternalStorage;
import static com.microsoft.appcenter.utils.storage.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
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
     * Random tool.
     */
    private static final Random RANDOM = new Random();

    /**
     * Initial maximum database size for some of the tests.
     */
    private static final long MAX_SIZE_IN_BYTES = 20480;

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Root path for private files.
     */
    private static String sAndroidFilesPath;

    /**
     * Database schema.
     */
    private static ContentValues mSchema;

    /**
     * Boolean value to simulate both true and false.
     */
    private static boolean mRandomBooleanValue = false;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
        sAndroidFilesPath = sContext.getFilesDir().getAbsolutePath() + "/test/";

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

        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath, filter);

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            InternalStorage.delete(sAndroidFilesPath + filename);
        }

        InternalStorage.delete(sAndroidFilesPath);

        /* Delete database. */
        sContext.deleteDatabase("test-databaseStorage");
        sContext.deleteDatabase("test-databaseStorageUpgrade");
        sContext.deleteDatabase("test-databaseStorageScannerRemove");
        sContext.deleteDatabase("test-databaseStorageScannerNext");
        sContext.deleteDatabase("test-databaseStorageInMemoryDB");
        sContext.deleteDatabase("test-setMaximumSize");
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
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        ContentValues values = new ContentValues();
        values.put("COL_STRING", new String(randomBytes));
        values.put("COL_STRING_NULL", (String) null);
        values.put("COL_BYTE", randomBytes[0]);
        values.put("COL_SHORT", (short) RANDOM.nextInt(100));
        values.put("COL_INTEGER", RANDOM.nextInt());
        values.put("COL_LONG", RANDOM.nextLong());
        values.put("COL_FLOAT", RANDOM.nextFloat());
        values.put("COL_DOUBLE", RANDOM.nextDouble());
        values.put("COL_BOOLEAN", (mRandomBooleanValue = !mRandomBooleanValue)/*RANDOM.nextBoolean()*/);
        values.put("COL_BYTE_ARRAY", randomBytes);
        return values;
    }

    public static void assertContentValuesEquals(ContentValues expected, ContentValues actual) {
        assertEquals(expected.getAsString("COL_STRING"), actual.getAsString("COL_STRING"));
        assertEquals(expected.getAsString("COL_STRING_NULL"), actual.getAsString("COL_STRING_NULL"));
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
        ContentValues value1 = generateContentValues();
        ContentValues value2 = generateContentValues();
        ContentValues value3 = generateContentValues();

        /* Put. */
        Long value1Id = databaseStorage.put(value1);
        assertNotNull(value1Id);

        /* Get for in-memory database test. */
        if (imdbTest) {

            /* Try with invalid key. */
            ContentValues valueFromDatabase = databaseStorage.get("COL_STRINGX", value1.getAsString("COL_STRING"));
            assertNull(valueFromDatabase);

            /* Try with valid key. */
            valueFromDatabase = databaseStorage.get("COL_STRING", value1.getAsString("COL_STRING"));
            assertContentValuesEquals(value1, valueFromDatabase);
            valueFromDatabase = databaseStorage.get("COL_STRING", value1.getAsString("COL_STRING") + "X");
            assertNull(valueFromDatabase);
        }

        /* Put another. */
        Long value2Id = databaseStorage.put(value2);
        assertNotNull(value2Id);

        /* Generate an ID that is neither value1Id nor value2Id. */

        /* Get. */
        ContentValues value1FromDatabase = databaseStorage.get(value1Id);
        assertContentValuesEquals(value1, value1FromDatabase);
        ContentValues value2FromDatabase = databaseStorage.get(DatabaseManager.PRIMARY_KEY, value2Id);
        assertContentValuesEquals(value2, value2FromDatabase);
        //noinspection ResourceType
        ContentValues nullValueFromDatabase = databaseStorage.get(-1);
        assertNull(nullValueFromDatabase);

        /* Count with scanner. */
        DatabaseStorage.DatabaseScanner scanner = databaseStorage.getScanner();
        assertEquals(2, scanner.getCount());
        assertEquals(2, scanner.getCount());
        DatabaseStorage.DatabaseScanner scanner1 = databaseStorage.getScanner("COL_STRING", value1.getAsString("COL_STRING"));
        assertEquals(1, scanner1.getCount());
        Iterator<ContentValues> iterator = scanner1.iterator();
        assertContentValuesEquals(value1, iterator.next());
        assertFalse(iterator.hasNext());

        /* Null value matching. */
        assertEquals(0, databaseStorage.getScanner("COL_STRING", null).getCount());
        assertEquals(2, databaseStorage.getScanner("COL_STRING_NULL", null).getCount());

        /* Test null value filter does not exclude anything, so returns the 2 logs. */
        scanner = databaseStorage.getScanner(null, null, "COL_STRING", null, false);
        assertEquals(2, scanner.getCount());

        /* Test filtering only with the second key parameter to get only the second log. */
        scanner = databaseStorage.getScanner(null, null, "COL_STRING", Collections.singletonList(value1.getAsString("COL_STRING")), false);
        assertEquals(1, scanner.getCount());
        assertContentValuesEquals(value2, scanner.iterator().next());

        /* Delete. */
        databaseStorage.delete(value1Id);
        assertNull(databaseStorage.get(value1Id));
        assertEquals(1, databaseStorage.size());
        assertEquals(1, databaseStorage.getScanner().getCount());

        /* Put logs to delete multiple IDs. */
        ContentValues value4 = generateContentValues();
        ContentValues value5 = generateContentValues();
        Long value4Id = databaseStorage.put(value4);
        Long value5Id = databaseStorage.put(value5);
        assertNotNull(value4Id);
        assertNotNull(value5Id);

        /* Delete multiple logs. */
        databaseStorage.delete(Arrays.asList(value4Id, value5Id));
        assertNull(databaseStorage.get(value4Id));
        assertNull(databaseStorage.get(value5Id));
        assertEquals(1, databaseStorage.size());

        /* Put logs to delete with condition. */
        ContentValues value6 = generateContentValues();
        ContentValues value7 = generateContentValues();
        value6.put("COL_STRING", value2.getAsString("COL_STRING"));
        value7.put("COL_STRING", value2.getAsString("COL_STRING") + "A");
        Long value6Id = databaseStorage.put(value6);
        Long value7Id = databaseStorage.put(value7);
        assertNotNull(value6Id);
        assertNotNull(value7Id);

        /* Delete for in-memory database test. */
        if (imdbTest) {

            /* Try with invalid key. */
            databaseStorage.delete("COL_STRINGX", value2.getAsString("COL_STRING"));
            assertEquals(3, databaseStorage.size());
        }

        /* Delete logs with condition. */
        databaseStorage.delete("COL_STRING", value2.getAsString("COL_STRING"));
        assertEquals(1, databaseStorage.size());
        ContentValues value7FromDatabase = databaseStorage.get(value7Id);
        assertContentValuesEquals(value7, value7FromDatabase);

        /* Clear. */
        databaseStorage.clear();
        assertEquals(0, databaseStorage.size());
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

        /* Test clear. */
        PreferencesStorage.putString("test", "someTest");
        PreferencesStorage.putInt("test2", 2);
        PreferencesStorage.clear();
        assertNull(PreferencesStorage.getString("test"));
        assertEquals(0, PreferencesStorage.getInt("test2"));
    }

    @Test
    public void internalStorage() throws IOException, InterruptedException {
        Log.i(TAG, "Testing Internal Storage file read/write");

        final String prefix = Long.toString(System.currentTimeMillis());

        /* Create a mock data. */
        String filename1 = prefix + "-" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION;
        String contents1 = "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.isEmpty()' on a null object reference\n" +
                "at com.microsoft.appcenter.utils.StorageHelperAndroidTest.internalStorage(StorageHelperAndroidTest.java:124)\n" +
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
        String filename4 = prefix + "-" + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION;

        /* FilenameFilter to look up files that are created in current test. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix) && filename.endsWith(INTERNAL_STORAGE_TEST_FILE_EXTENSION);
            }
        };

        /* Write contents to test files after 2 sec delay. */
        Log.i(TAG, "Writing " + filename1);
        InternalStorage.write(sAndroidFilesPath + filename1, contents1);
        TimeUnit.SECONDS.sleep(2);
        Log.i(TAG, "Writing " + filename2);
        InternalStorage.write(sAndroidFilesPath + filename2, contents2);
        /* Also write empty content to a test file. */
        InternalStorage.write(sAndroidFilesPath + filename3, "");
        InternalStorage.write(sAndroidFilesPath + filename4, "  ");

        /* Get file names in the root path. */
        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath, filter);

        /* Verify the files are created. */
        assertNotNull(filenames);
        assertEquals(2, filenames.length);
        List<String> list = Arrays.asList(filenames);
        assertTrue(list.contains(filename1));
        assertTrue(list.contains(filename2));
        assertFalse(list.contains(filename3));
        assertFalse(list.contains(filename4));

        /* Get the most recent file. */
        File lastModifiedFile = InternalStorage.lastModifiedFile(sAndroidFilesPath, filter);

        /* Verify the most recent file. */
        assertNotNull(lastModifiedFile);
        assertEquals(filename2, lastModifiedFile.getName());

        /* Read the most recent file. */
        String actual = InternalStorage.read(lastModifiedFile);

        /* Verify the contents of the most recent file. */
        assertNotNull(actual);
        assertEquals(contents2, actual.trim());

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            Log.i(TAG, "Deleting " + filename);
            assertTrue(InternalStorage.delete(sAndroidFilesPath + filename));
        }

        /* Verify all the files are properly deleted. */
        assertEquals(0, InternalStorage.getFilenames(sAndroidFilesPath, filter).length);

        /* Verify invalid accesses. */
        assertNull(InternalStorage.read("not-exist-filename"));
        assertArrayEquals(new String[0], InternalStorage.getFilenames("not-exist-path", null));
        assertNull(InternalStorage.lastModifiedFile("not-exist-path", null));
    }

    @Test
    public void internalStorageForObject() throws IOException, ClassNotFoundException {
        Log.i(TAG, "Testing Internal Storage object serialization");

        File file = new File(sAndroidFilesPath + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION);

        /* Create a mock object. */
        DataModel model = new DataModel(10, "Model", true);

        /* Write the object to a file. */
        InternalStorage.writeObject(file, model);

        /* Read the file. */
        DataModel actual = InternalStorage.readObject(file);

        /* Read with class cast exception. */
        Exception readCastException = null;
        try {
            @SuppressWarnings("UnusedAssignment")
            String wrongType = InternalStorage.readObject(file);
        } catch (Exception e) {
            readCastException = e;
        }
        assertTrue(readCastException instanceof ClassCastException);

        /* Verify the deserialized instance and original instance are same. */
        assertNotNull(actual);
        assertEquals(model.number, actual.number);
        assertEquals(model.object.text, actual.object.text);
        assertEquals(model.object.enabled, actual.object.enabled);

        /* Delete the files to clean up. */
        Log.i(TAG, "Deleting " + file.getName());
        InternalStorage.delete(file);
    }

    @Test
    public void internalStorageForBytes() throws IOException {
        Log.i(TAG, "Testing Internal Storage bytes read/write");

        File file = new File(sAndroidFilesPath + UUIDUtils.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENSION);

        /* Create a mock object. */
        String hello = "Hello world";
        StorageHelper.InternalStorage.write(file, hello);

        /* Read the file as bytes. */
        byte[] helloBytes = StorageHelper.InternalStorage.readBytes(file);

        /* Check. */
        assertNotNull(helloBytes);
        assertEquals(hello, new String(helloBytes, "UTF-8"));

        /* Delete the files to clean up. */
        Log.i(TAG, "Deleting " + file.getName());
        InternalStorage.delete(file);

        /* Check file not found. */
        assertNull(StorageHelper.InternalStorage.readBytes(file));
    }

    @Test
    public void databaseStorage() {
        Log.i(TAG, "Testing Database Storage");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorage", "databaseStorage", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

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
            //noinspection ThrowFromFinallyBlock
            databaseStorage.close();
        }
    }

    @Test
    public void databaseStorageUpgradeNotHandled() {
        Log.i(TAG, "Testing Database Storage Upgrade by recreating table");

        /* Create a schema for v1. */
        ContentValues schema = new ContentValues();
        schema.put("COL_STRING", "");

        /* Create a row for v1. */
        ContentValues oldVersionValue = new ContentValues();
        oldVersionValue.put("COL_STRING", "Hello World");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 1, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });
        try {

            /* Database will always create a column for identifiers so default length of all tables is 1. */
            assertEquals(2, databaseStorage.getColumnNames().length);
            long id = databaseStorage.put(oldVersionValue);

            /* Put data. */
            ContentValues actual = databaseStorage.get(id);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseStorage.size());
        } finally {

            /* Close. */
            databaseStorage.close();
        }

        /* Get instance to access database with a newer schema without handling upgrade. */
        databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 2, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        /* Verify data deleted since no handled upgrade. */
        try {
            assertEquals(11, databaseStorage.getColumnNames().length);
            assertEquals(0, databaseStorage.size());
        } finally {

            /* Close. */
            databaseStorage.close();
        }
    }

    @Test
    public void databaseStorageUpgradeHandled() {
        Log.i(TAG, "Testing Database Storage Upgrade by updating table");

        /* Create a schema for v1. */
        ContentValues schema = new ContentValues();
        schema.put("COL_STRING", "");

        /* Create a row for v1. */
        ContentValues oldVersionValue = new ContentValues();
        oldVersionValue.put("COL_STRING", "Hello World");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 1, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });

        /* Put data. */
        long id;
        try {
            id = databaseStorage.put(oldVersionValue);
            ContentValues actual = databaseStorage.get(id);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseStorage.size());
        } finally {

            /* Close. */
            databaseStorage.close();
        }

        /* Upgrade schema. */
        schema.put("COL_INT", 1);

        /* Get instance to access database with a newer schema without handling upgrade. */
        databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageUpgrade", "databaseStorageUpgrade", 2, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("ALTER TABLE databaseStorageUpgrade ADD COLUMN COL_INT INTEGER");
                return true;
            }

            @Override
            public void onError(String operation, RuntimeException e) {
                throw e;
            }
        });
        try {

            /* Verify data still there. */
            ContentValues actual = databaseStorage.get(id);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseStorage.size());

            /* Put new data. */
            ContentValues data = new ContentValues();
            data.put("COL_STRING", "Hello World");
            data.put("COL_INT", 2);
            id = databaseStorage.put(data);
            actual = databaseStorage.get(id);
            actual.remove("oid");
            assertEquals(data, actual);
            assertEquals(2, databaseStorage.size());
        } finally {

            /* Close. */
            databaseStorage.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void databaseStorageScannerRemove() {
        Log.i(TAG, "Testing Database Storage Exceptions");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageScannerRemove", "databaseStorageScannerRemove", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

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
            //noinspection ThrowFromFinallyBlock
            databaseStorage.close();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void databaseStorageScannerNext() {
        Log.i(TAG, "Testing Database Storage Exceptions");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageScannerNext", "databaseStorageScannerNext", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

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
            //noinspection ThrowFromFinallyBlock
            databaseStorage.close();
        }
    }

    /* This is a hack to test database failure by passing a weird table name which is actually valid.
       SQLite database allows to create a table that contains period (.) but it doesn't actually create the table and doesn't raise any exceptions.
       This test method will then be able to test in-memory database by accessing a table which is not created.
       Only tested on emulator so it might not work in the future or on any other devices. */
    @Test
    public void databaseStorageInMemoryDB() {
        Log.i(TAG, "Testing Database Storage switch over to in-memory database");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-databaseStorageInMemoryDB", "test.databaseStorageInMemoryDB", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

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
            //noinspection ThrowFromFinallyBlock
            databaseStorage.close();
        }
    }

    @Test
    public void setMaximumSize() {
        Log.i(TAG, "Testing Database Storage set maximum size");

        /* Get instance to access database. */
        DatabaseStorage databaseStorage = DatabaseStorage.getDatabaseStorage("test-setMaximumSize", "test.setMaximumSize", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }

            @Override
            public void onError(String operation, RuntimeException e) {

                /* Do not handle any errors. This is simulating errors so this is expected. */
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {

            /* Test to change to an exact size as its multiple of 4KB. */
            assertTrue(databaseStorage.setMaxStorageSize(MAX_SIZE_IN_BYTES));
            assertEquals(MAX_SIZE_IN_BYTES, databaseStorage.getMaxSize());

            /* Test inexact value, it will use next multiple of 4KB. */
            long desiredSize = MAX_SIZE_IN_BYTES * 2 + 1;
            assertTrue(databaseStorage.setMaxStorageSize(desiredSize));
            assertEquals(desiredSize - 1 + ALLOWED_SIZE_MULTIPLE, databaseStorage.getMaxSize());

            /* Try to set to a very small value. */
            assertFalse(databaseStorage.setMaxStorageSize(2));

            /* Test the side effect is that we shrunk to the minimum size that is possible. */
            assertEquals(MAX_SIZE_IN_BYTES, databaseStorage.getMaxSize());
        } finally {

            /* Close. */
            //noinspection ThrowFromFinallyBlock
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