/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileManagerAndroidTest {

    /**
     * File extension.
     */
    private static final String FILE_STORAGE_TEST_FILE_EXTENSION = ".stacktrace";

    /**
     * Root path for private files.
     */
    private static String sAndroidFilesPath;

    @BeforeClass
    public static void setUpClass() {
        Context context = InstrumentationRegistry.getTargetContext();
        FileManager.initialize(context);
        sAndroidFilesPath = context.getFilesDir().getAbsolutePath() + "/test/";

        /* Create a test directory. */
        FileManager.mkdir(sAndroidFilesPath);
    }

    @AfterClass
    public static void tearDownClass() {

        /* Clean up file storage. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FILE_STORAGE_TEST_FILE_EXTENSION);
            }
        };

        String[] filenames = FileManager.getFilenames(sAndroidFilesPath, filter);

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            FileManager.delete(sAndroidFilesPath + filename);
        }

        FileManager.delete(sAndroidFilesPath);
    }

    @Test
    public void fileManager() throws IOException, InterruptedException {
        final String prefix = Long.toString(System.currentTimeMillis());

        /* Create a mock data. */
        String filename1 = prefix + "-" + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION;
        String contents1 = "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.isEmpty()' on a null object reference\n" +
                "at com.microsoft.appcenter.utils.StorageHelperAndroidTest.internalStorage(StorageHelperAndroidTest.java:124)\n" +
                "at java.lang.reflect.Method.invoke(Native Method)\n" +
                "at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)";
        String filename2 = prefix + "-" + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION;
        //noinspection SpellCheckingInspection
        String contents2 = "java.io.FileNotFoundException: 6c1b1c58-1c2f-47d9-8f04-52639c3a804d: open failed: EROFS (Read-only file system)\n" +
                "at libcore.io.IoBridge.open(IoBridge.java:452)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:87)\n" +
                "at java.io.FileOutputStream.<init>(FileOutputStream.java:72)\n" +
                "at java.io.FileWriter.<init>(FileWriter.java:42)";
        String filename3 = prefix + "-" + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION;
        String filename4 = prefix + "-" + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION;

        /* FilenameFilter to look up files that are created in current test. */
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix) && filename.endsWith(FILE_STORAGE_TEST_FILE_EXTENSION);
            }
        };

        /* Write contents to test files after 2 sec delay. */
        FileManager.write(sAndroidFilesPath + filename1, contents1);
        TimeUnit.SECONDS.sleep(2);
        FileManager.write(sAndroidFilesPath + filename2, contents2);

        /* Also write empty content to a test file. */
        FileManager.write(sAndroidFilesPath + filename3, "");
        FileManager.write(sAndroidFilesPath + filename4, "  ");

        /* Get file names in the root path. */
        String[] filenames = FileManager.getFilenames(sAndroidFilesPath, filter);

        /* Verify the files are created. */
        assertNotNull(filenames);
        assertEquals(2, filenames.length);
        List<String> list = Arrays.asList(filenames);
        assertTrue(list.contains(filename1));
        assertTrue(list.contains(filename2));
        assertFalse(list.contains(filename3));
        assertFalse(list.contains(filename4));

        /* Get the most recent file. */
        File lastModifiedFile = FileManager.lastModifiedFile(sAndroidFilesPath, filter);

        /* Verify the most recent file. */
        assertNotNull(lastModifiedFile);
        assertEquals(filename2, lastModifiedFile.getName());

        /* Read the most recent file. */
        String actual = FileManager.read(lastModifiedFile);

        /* Verify the contents of the most recent file. */
        assertNotNull(actual);
        assertEquals(contents2, actual.trim());

        /* Delete the files to clean up. */
        for (String filename : filenames) {
            assertTrue(FileManager.delete(sAndroidFilesPath + filename));
        }

        /* Verify all the files are properly deleted. */
        assertEquals(0, FileManager.getFilenames(sAndroidFilesPath, filter).length);

        /* Verify invalid accesses. */
        assertNull(FileManager.read("not-exist-filename"));
        assertArrayEquals(new String[0], FileManager.getFilenames("not-exist-path", null));
        assertNull(FileManager.lastModifiedFile("not-exist-path", null));
    }

    @Test
    public void fileManagerForObject() throws IOException, ClassNotFoundException {
        File file = new File(sAndroidFilesPath + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION);

        /* Create a mock object. */
        DataModel model = new DataModel(10, "Model", true);

        /* Write the object to a file. */
        FileManager.writeObject(file, model);

        /* Read the file. */
        DataModel actual = FileManager.readObject(file);

        /* Read with class cast exception. */
        Exception readCastException = null;
        try {

            @SuppressWarnings("unused")
            String wrongType = FileManager.readObject(file);
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
        FileManager.delete(file);
    }

    @Test
    public void fileManagerForBytes() throws IOException {
        File file = new File(sAndroidFilesPath + UUID.randomUUID().toString() + FILE_STORAGE_TEST_FILE_EXTENSION);

        /* Create a mock object. */
        String hello = "Hello world";
        FileManager.write(file, hello);

        /* Read the file as bytes. */
        byte[] helloBytes = FileManager.readBytes(file);

        /* Check. */
        assertNotNull(helloBytes);
        assertEquals(hello, new String(helloBytes, StandardCharsets.UTF_8));

        /* Delete the files to clean up. */
        FileManager.delete(file);

        /* Check file not found. */
        assertNull(FileManager.readBytes(file));
    }

    /**
     * Temporary class for testing object serialization.
     */
    private static class DataModel implements Serializable {
        final int number;
        final InnerModel object;

        @SuppressWarnings("SameParameterValue")
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