package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.Context;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.utils.storage.StorageHelper.InternalStorage;
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
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Root path for private files.
     */
    private static String sAndroidFilesPath;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
        SharedPreferencesManager.initialize(sContext);
        sAndroidFilesPath = sContext.getFilesDir().getAbsolutePath() + "/test/";

        /* Create a test directory. */
        InternalStorage.mkdir(sAndroidFilesPath);
    }

    @AfterClass
    public static void tearDownClass() {

        /* Clean up shared preferences. */
        try {
            for (SharedPreferencesTestData data : generateSharedPreferenceData()) {
                String key = data.value.getClass().getCanonicalName();
                SharedPreferencesManager.remove(key);
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
    }

    private static SharedPreferencesTestData[] generateSharedPreferenceData() throws NoSuchMethodException {
        SharedPreferencesTestData[] testData = new SharedPreferencesTestData[6];

        /* boolean */
        testData[0] = new SharedPreferencesTestData();
        testData[0].value = true;
        testData[0].defaultValue = false;
        testData[0].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getBoolean", String.class);
        testData[0].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getBoolean", String.class, boolean.class);
        testData[0].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putBoolean", String.class, boolean.class);

        /* float */
        testData[1] = new SharedPreferencesTestData();
        testData[1].value = 111.22f;
        testData[1].defaultValue = 0.01f;
        testData[1].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getFloat", String.class);
        testData[1].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getFloat", String.class, float.class);
        testData[1].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putFloat", String.class, float.class);

        /* int */
        testData[2] = new SharedPreferencesTestData();
        testData[2].value = 123;
        testData[2].defaultValue = -1;
        testData[2].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getInt", String.class);
        testData[2].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getInt", String.class, int.class);
        testData[2].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putInt", String.class, int.class);

        /* long */
        testData[3] = new SharedPreferencesTestData();
        testData[3].value = 123456789000L;
        testData[3].defaultValue = 345L;
        testData[3].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getLong", String.class);
        testData[3].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getLong", String.class, long.class);
        testData[3].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putLong", String.class, long.class);

        /* String */
        testData[4] = new SharedPreferencesTestData();
        testData[4].value = "Hello World";
        testData[4].defaultValue = "Empty";
        testData[4].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getString", String.class);
        testData[4].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getString", String.class, String.class);
        testData[4].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putString", String.class, String.class);

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
        testData[5].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getStringSet", String.class);
        testData[5].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getStringSet", String.class, Set.class);
        testData[5].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putStringSet", String.class, Set.class);

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
            Object actual = data.getMethod1.invoke(null, key);

            /* Verify the value is same as assigned. */
            assertEquals(data.value, actual);

            /* Remove key from shared preferences. */
            SharedPreferencesManager.remove(key);

            /* Verify the value equals to default value. */
            assertEquals(data.defaultValue, data.getMethod2.invoke(null, key, data.defaultValue));
        }

        /* Test clear. */
        SharedPreferencesManager.putString("test", "someTest");
        SharedPreferencesManager.putInt("test2", 2);
        SharedPreferencesManager.clear();
        assertNull(SharedPreferencesManager.getString("test"));
        assertEquals(0, SharedPreferencesManager.getInt("test2"));
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