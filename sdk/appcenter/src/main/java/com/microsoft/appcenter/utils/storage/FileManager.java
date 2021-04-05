/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * File manager for internal/external storage access
 */
public class FileManager {

    /**
     * Application context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Initializes FileManager class.
     *
     * @param context The context of the application.
     */
    public static synchronized void initialize(Context context) {
        if (sContext == null) {
            sContext = context;
        }
    }

    /**
     * Read contents from a file.
     *
     * @param path The path of the file.
     * @return The contents of the file.
     */
    @SuppressWarnings("SameParameterValue")
    public static String read(@NonNull String path) {
        return read(new File(path));
    }

    /**
     * Read contents from a file.
     *
     * @param file The file to read from.
     * @return The contents of the file.
     */
    public static String read(@NonNull File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder contents;

            //noinspection TryFinallyCanBeTryWithResources (requires min API level 19)
            try {
                String line;
                String lineSeparator = System.getProperty("line.separator");
                contents = new StringBuilder();
                line = reader.readLine();
                if (line != null) {
                    contents.append(line);
                    while ((line = reader.readLine()) != null) {
                        contents.append(lineSeparator).append(line);
                    }
                }
            } finally {
                reader.close();
            }
            return contents.toString();
        } catch (IOException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Could not read file " + file.getAbsolutePath(), e);
        }
        return null;
    }

    /**
     * Read contents from a file into byte array.
     *
     * @param file The file to read from.
     * @return The contents of the file.
     */
    public static byte[] readBytes(@NonNull File file) {
        byte[] fileContents = new byte[(int) file.length()];
        try {
            FileInputStream fileStream = new FileInputStream(file);

            //noinspection TryFinallyCanBeTryWithResources
            try {
                DataInputStream dataInputStream = new DataInputStream(fileStream);
                dataInputStream.readFully(fileContents);
                return fileContents;
            } finally {
                fileStream.close();
            }
        } catch (IOException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Could not read file " + file.getAbsolutePath(), e);
        }
        return null;
    }

    /**
     * Write contents to a file.
     *
     * @param path     The path of the file.
     * @param contents The contents to be written to the file.
     * @throws IOException If an I/O error occurs
     */
    public static void write(@NonNull String path, @NonNull String contents) throws IOException {
        write(new File(path), contents);
    }

    /**
     * Write contents to a file.
     *
     * @param file     The file instance.
     * @param contents The content to be written to the file. Must not be empty or whitespace only.
     * @throws IOException If an I/O error occurs
     */
    public static void write(@NonNull File file, @NonNull String contents) throws IOException {
        if (TextUtils.isEmpty(contents) || TextUtils.getTrimmedLength(contents) <= 0) {
            return;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        //noinspection TryFinallyCanBeTryWithResources
        try {
            writer.write(contents);
        } finally {
            writer.close();
        }
    }

    /**
     * Get an array of filenames in the path.
     *
     * @param path   The directory path.
     * @param filter The filter to match file names against, may be {@code null}.
     * @return An array of filename that doesn't include paths.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static String[] getFilenames(@NonNull String path, @Nullable FilenameFilter filter) {
        File dir = new File(path);
        if (dir.exists()) {
            return dir.list(filter);
        }

        return new String[0];
    }

    /**
     * Get the most recently modified file in the directory specified.
     *
     * @param path   The directory path.
     * @param filter The filter to match file names against, may be {@code null}.
     * @return The last modified file in the directory matching the specified filter, if any matches. {@code null} otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static File lastModifiedFile(@NonNull String path, @Nullable FilenameFilter filter) {
        return lastModifiedFile(new File(path), filter);
    }

    /**
     * Get the most recently modified file in the directory specified.
     *
     * @param dir    The directory.
     * @param filter The filter to match file names against, may be {@code null}.
     * @return The last modified file in the directory matching the specified filter, if any matches. {@code null} otherwise.
     */
    @Nullable
    public static File lastModifiedFile(@NonNull File dir, @Nullable FilenameFilter filter) {
        if (dir.exists()) {
            File[] files = dir.listFiles(filter);
            long lastModification = 0;
            File lastModifiedFile = null;

            if (files != null) {
                for (File file : files) {
                    if (file.lastModified() > lastModification) {
                        lastModification = file.lastModified();
                        lastModifiedFile = file;
                    }
                }

                return lastModifiedFile;
            }
        }

        return null;
    }

    /**
     * Delete a file or directory with the given path.
     *
     * @param path The path of the file or directory.
     * @return {@code true} if it was deleted, {@code false} otherwise.
     */
    public static boolean delete(@NonNull String path) {
        return delete(new File(path));
    }

    /**
     * Delete a file or directory.
     *
     * @param file The file or directory to delete.
     * @return {@code true} if it was deleted, {@code false} otherwise.
     */
    public static boolean delete(@NonNull File file) {
        return file.delete();
    }

    /**
     * Delete a directory with files.
     *
     * @param file The file or directory to delete.
     * @return {@code true} if it was deleted, {@code false} otherwise.
     */
    public static boolean deleteDirectory(@NonNull File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        return file.delete();
    }

    /**
     * Delete all files inside this directory, without deleting directory itself.
     *
     * @param directory The directory to delete.
     */
    public static void cleanDirectory(@NonNull File directory) {
        File[] contents = directory.listFiles();
        if (contents != null) {
            for (File file : contents) {
                deleteDirectory(file);
            }
        }
    }

    /**
     * Create a directory if it does not already exist.
     * Will create the whole directory tree if necessary.
     *
     * @param path An absolute path for the directory to be created.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void mkdir(@NonNull String path) {
        new File(path).mkdirs();
    }
}
