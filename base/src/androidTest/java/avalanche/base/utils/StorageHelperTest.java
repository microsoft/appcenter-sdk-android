package avalanche.base.utils;

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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static avalanche.base.utils.StorageHelper.InternalStorage;
import static avalanche.base.utils.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class StorageHelperTest {
    /**
     * Log tag.
     */
    private static final String TAG = "StorageHelperTest";

    /**
     * File extension.
     */
    private static final String INTERNAL_STORAGE_TEST_FILE_EXTENTION = ".stacktrace";

    /**
     * Context instance.
     */
    private static Context sContext;

    /**
     * Root path for private files.
     */
    private static String sAndroidFilesPath;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
        sAndroidFilesPath = sContext.getFilesDir().getAbsolutePath();
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
                return filename.endsWith(INTERNAL_STORAGE_TEST_FILE_EXTENTION);
            }
        };

        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }
    }

    private static SharedPreferencesTestData[] generateSharedPreferenceData() throws NoSuchMethodException {
        SharedPreferencesTestData[] testData = new SharedPreferencesTestData[6];

        /* boolean */
        testData[0] = new SharedPreferencesTestData();
        testData[0].value = true;
        testData[0].defaultValue = false;
        testData[0].getMethod = PreferencesStorage.class.getDeclaredMethod("getBoolean", String.class, boolean.class);
        testData[0].putMethod = PreferencesStorage.class.getDeclaredMethod("putBoolean", String.class, boolean.class);

        /* float */
        testData[1] = new SharedPreferencesTestData();
        testData[1].value = 111.22f;
        testData[1].defaultValue = 0f;
        testData[1].getMethod = PreferencesStorage.class.getDeclaredMethod("getFloat", String.class, float.class);
        testData[1].putMethod = PreferencesStorage.class.getDeclaredMethod("putFloat", String.class, float.class);

        /* int */
        testData[2] = new SharedPreferencesTestData();
        testData[2].value = 123;
        testData[2].defaultValue = 0;
        testData[2].getMethod = PreferencesStorage.class.getDeclaredMethod("getInt", String.class, int.class);
        testData[2].putMethod = PreferencesStorage.class.getDeclaredMethod("putInt", String.class, int.class);

        /* long */
        testData[3] = new SharedPreferencesTestData();
        testData[3].value = 123456789000L;
        testData[3].defaultValue = 0L;
        testData[3].getMethod = PreferencesStorage.class.getDeclaredMethod("getLong", String.class, long.class);
        testData[3].putMethod = PreferencesStorage.class.getDeclaredMethod("putLong", String.class, long.class);

        /* String */
        testData[4] = new SharedPreferencesTestData();
        testData[4].value = "Hello World";
        testData[4].defaultValue = null;
        testData[4].getMethod = PreferencesStorage.class.getDeclaredMethod("getString", String.class, String.class);
        testData[4].putMethod = PreferencesStorage.class.getDeclaredMethod("putString", String.class, String.class);

        /* Set<String> */
        Set<String> data = new HashSet<>();
        data.add("ABC");
        data.add("Hello World");
        data.add("Welcome to the world!");

        testData[5] = new SharedPreferencesTestData();
        testData[5].value = data;
        testData[5].defaultValue = null;
        testData[5].getMethod = PreferencesStorage.class.getDeclaredMethod("getStringSet", String.class, Set.class);
        testData[5].putMethod = PreferencesStorage.class.getDeclaredMethod("putStringSet", String.class, Set.class);

        return testData;
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
            Object actual = data.getMethod.invoke(null, key, data.defaultValue);

            /* Check the value is same as assigned. */
            assertEquals(data.value, actual);

            /* Remove key from shared preferences. */
            PreferencesStorage.remove(key);

            /* Check the value equals to default value. */
            assertEquals(data.defaultValue, data.getMethod.invoke(null, key, data.defaultValue));
        }
    }

    @Test
    public void installId() {
        Log.i(TAG, "Testing installId-shortcut");
        UUID expected = UUID.randomUUID();
        PreferencesStorage.putString(PreferencesStorage.SHARED_PREFS_INSTALL_ID, expected.toString());

        UUID actual = PreferencesStorage.getInstallId();
        assertEquals(expected, actual);

        String wrongUUID = "1234567";
        PreferencesStorage.putString(PreferencesStorage.SHARED_PREFS_INSTALL_ID, expected.toString());

        actual = PreferencesStorage.getInstallId();
        assertNotEquals(wrongUUID, actual);
    }

    @Test
    public void internalStorage() throws IOException, InterruptedException {
        Log.i(TAG, "Testing Internal Storage file read/write");

        final String prefix = Long.toString(System.currentTimeMillis());

        /* Create a mock data. */
        String filename1 = prefix + "-" + UUID.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENTION;
        String contents1 = "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.isEmpty()' on a null object reference\n" +
                "at avalanche.base.utils.StorageHelperTest.internalStorage(StorageHelperTest.java:124)\n" +
                "at java.lang.reflect.Method.invoke(Native Method)\n" +
                "at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)";
        String filename2 = prefix + "-" + UUID.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENTION;
        String contents2 = "java.io.FileNotFoundException: 6c1b1c58-1c2f-47d9-8f04-52639c3a804d: open failed: EROFS (Read-only file system)\n" +
                "at libcore.io.IoBridge.open(IoBridge.java:452)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:87)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:72)\n" +
                "at java.io.FileWriter.<init>(FileWriter.java:42)";

        /* FilenameFilter to look up files that are created in current test. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix) && filename.endsWith(INTERNAL_STORAGE_TEST_FILE_EXTENTION);
            }
        };

        /* Write contents to test files after 2 sec delay. */
        Log.i(TAG, "Writing " + filename1);
        InternalStorage.write(sAndroidFilesPath + "/" + filename1, contents1);
        TimeUnit.SECONDS.sleep(2);
        Log.i(TAG, "Writing " + filename2);
        InternalStorage.write(sAndroidFilesPath + "/" + filename2, contents2);

        /* Get file names in the root path. */
        String[] filenames = InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Check the files are created. */
        assertNotNull(filenames);
        assertEquals(2, filenames.length);

        /* Get the most recent file. */
        File lastModifiedFile = InternalStorage.lastModifiedFile(sAndroidFilesPath, filter);

        /* Check the most recent file. */
        assertNotNull(lastModifiedFile);
        assertEquals(filename2, lastModifiedFile.getName());

        /* Read the most recent file. */
        String actual = InternalStorage.read(lastModifiedFile);

        /* Check the contents of the most recent file. */
        assertEquals(contents2, actual.trim());

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            Log.i(TAG, "Deleting " + filename);
            InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }

        /* Check all the files are properly deleted. */
        assertEquals(0, InternalStorage.getFilenames(sAndroidFilesPath, filter).length);
    }

    @Test
    public void internalStorageForObject() throws IOException, ClassNotFoundException {
        Log.i(TAG, "Testing Internal Storage object serialization");

        File file = new File(sAndroidFilesPath + "/" + UUID.randomUUID().toString() + INTERNAL_STORAGE_TEST_FILE_EXTENTION);

        /* Create a mock object. */
        DataModel model = new DataModel(10, "Model", true);

        /* Write the object to a file. */
        InternalStorage.writeObject(file, model);

        /* Read the file. */
        DataModel actual = InternalStorage.readObject(file, DataModel.class);

        /* Check the deserialized instance and original instance are same. */
        assertNotNull(actual);
        assertEquals(model.number, actual.number);
        assertEquals(model.object.text, actual.object.text);
        assertEquals(model.object.enabled, actual.object.enabled);

        /* Delete the files to clean up. */
        Log.i(TAG, "Deleting " + file.getName());
        InternalStorage.delete(file);
    }

    /**
     * Temporary class for testing shared preferences.
     */
    private static class SharedPreferencesTestData {
        protected Object value;
        protected Object defaultValue;
        protected Method getMethod;
        protected Method putMethod;
    }

    /**
     * Temporary class for testing object serialization.
     */
    private static class DataModel implements Serializable {
        protected int number;
        protected InnerModel object;

        protected DataModel(int number, String text, boolean enabled) {
            this.number = number;
            this.object = new InnerModel(text, enabled);
        }

        protected static class InnerModel implements Serializable {
            protected String text;
            protected boolean enabled;

            protected InnerModel(String text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }
    }
}