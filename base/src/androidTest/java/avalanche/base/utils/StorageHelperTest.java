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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
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
                StorageHelper.SharedPreferences.remove(key);
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

        String[] filenames = StorageHelper.InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            StorageHelper.InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }
    }

    private static SharedPreferencesTestData[] generateSharedPreferenceData() throws NoSuchMethodException {
        SharedPreferencesTestData[] testData = new SharedPreferencesTestData[6];

        /* boolean */
        testData[0] = new SharedPreferencesTestData();
        testData[0].value = true;
        testData[0].defaultValue = false;
        testData[0].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getBoolean", String.class, boolean.class);
        testData[0].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putBoolean", String.class, boolean.class);

        /* float */
        testData[1] = new SharedPreferencesTestData();
        testData[1].value = 111.22f;
        testData[1].defaultValue = 0f;
        testData[1].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getFloat", String.class, float.class);
        testData[1].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putFloat", String.class, float.class);

        /* int */
        testData[2] = new SharedPreferencesTestData();
        testData[2].value = 123;
        testData[2].defaultValue = 0;
        testData[2].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getInt", String.class, int.class);
        testData[2].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putInt", String.class, int.class);

        /* long */
        testData[3] = new SharedPreferencesTestData();
        testData[3].value = 123456789000L;
        testData[3].defaultValue = 0L;
        testData[3].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getLong", String.class, long.class);
        testData[3].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putLong", String.class, long.class);

        /* String */
        testData[4] = new SharedPreferencesTestData();
        testData[4].value = "Hello World";
        testData[4].defaultValue = null;
        testData[4].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getString", String.class, String.class);
        testData[4].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putString", String.class, String.class);

        /* Set<String> */
        Set<String> data = new HashSet<>();
        data.add("ABC");
        data.add("Hello World");
        data.add("Welcome to the world!");

        testData[5] = new SharedPreferencesTestData();
        testData[5].value = data;
        testData[5].defaultValue = null;
        testData[5].getMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("getStringSet", String.class, Set.class);
        testData[5].putMethod = StorageHelper.SharedPreferences.class.getDeclaredMethod("putStringSet", String.class, Set.class);

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
            StorageHelper.SharedPreferences.remove(key);

            /* Check the value equals to default value. */
            assertEquals(data.defaultValue, data.getMethod.invoke(null, key, data.defaultValue));
        }
    }

    @Test
    public void internalStorage() throws IOException, InterruptedException {
        Log.i(TAG, "Testing Internal Storage");

        final String prefix = Long.toString(System.currentTimeMillis());
        final String ext = ".stacktrace";

        /* Create a mock data. */
        String filename1 = prefix + "-" + UUID.randomUUID().toString() + ext;
        String contents1 = "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.isEmpty()' on a null object reference\n" +
                "at avalanche.base.utils.StorageHelperTest.internalStorage(StorageHelperTest.java:124)\n" +
                "at java.lang.reflect.Method.invoke(Native Method)\n" +
                "at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)";
        String filename2 = prefix + "-" + UUID.randomUUID().toString() + ext;
        String contents2 = "java.io.FileNotFoundException: 6c1b1c58-1c2f-47d9-8f04-52639c3a804d: open failed: EROFS (Read-only file system)\n" +
                "at libcore.io.IoBridge.open(IoBridge.java:452)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:87)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:72)\n" +
                "at java.io.FileWriter.<init>(FileWriter.java:42)";

        /* FilenameFilter to look up files that are created in current test. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix) && filename.endsWith(ext);
            }
        };

        /* Write contents to test files after 2 sec delay. */
        Log.i(TAG, "Writing " + filename1);
        StorageHelper.InternalStorage.write(sAndroidFilesPath + "/" + filename1, contents1);
        TimeUnit.SECONDS.sleep(2);
        Log.i(TAG, "Writing " + filename2);
        StorageHelper.InternalStorage.write(sAndroidFilesPath + "/" + filename2, contents2);

        /* Get file names in the root path. */
        String[] filenames = StorageHelper.InternalStorage.getFilenames(sAndroidFilesPath + "/", filter);

        /* Check the files are created. */
        assertNotNull(filenames);
        assertEquals(2, filenames.length);

        /* Get the most recent file. */
        File lastModifiedFile = StorageHelper.InternalStorage.lastModifiedFile(sAndroidFilesPath, filter);

        /* Check the most recent file. */
        assertNotNull(lastModifiedFile);
        assertEquals(filename2, lastModifiedFile.getName());

        /* Read the most recent file. */
        String actual = StorageHelper.InternalStorage.read(lastModifiedFile);

        /* Check the contents of the most recent file. */
        assertEquals(contents2, actual.trim());

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            Log.i(TAG, "Deleting " + filename);
            StorageHelper.InternalStorage.delete(sAndroidFilesPath + "/" + filename);
        }

        /* Check all the files are properly deleted. */
        assertEquals(0, StorageHelper.InternalStorage.getFilenames(sAndroidFilesPath, filter).length);
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
}