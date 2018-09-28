package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * StorageHelper class to access local storage.
 */
public class StorageHelper {

    /**
     * Name of preferences.
     */
    private static final String PREFERENCES_NAME = "AppCenter";

    /**
     * Application context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Android SharedPreferences instance.
     */
    private static SharedPreferences sSharedPreferences;

    /**
     * Initializes StorageHelper class.
     *
     * @param context The context of the application.
     */
    public static synchronized void initialize(Context context) {
        if (sContext == null) {
            sContext = context;
            sSharedPreferences = sContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * PreferencesStorage Helper class
     */
    public static class PreferencesStorage {

        /**
         * Retrieve a boolean value.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or false if key is not set.
         */
        @SuppressWarnings("unused")
        public static boolean getBoolean(@NonNull String key) {
            return getBoolean(key, false);
        }

        /**
         * Retrieve a boolean value and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        public static boolean getBoolean(@NonNull String key, boolean defValue) {
            return sSharedPreferences.getBoolean(key, defValue);
        }

        /**
         * Store a boolean value.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        public static void putBoolean(@NonNull String key, boolean value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }

        /**
         * Retrieve a float value.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or 0f if key is not set.
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public static float getFloat(@NonNull String key) {
            return getFloat(key, 0f);
        }

        /**
         * Retrieve a float value and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static float getFloat(@NonNull String key, float defValue) {
            return sSharedPreferences.getFloat(key, defValue);
        }

        /**
         * Store a float value.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public static void putFloat(@NonNull String key, float value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putFloat(key, value);
            editor.apply();
        }

        /**
         * Retrieve an int value.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or 0 if key is not set.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static int getInt(@NonNull String key) {
            return getInt(key, 0);
        }

        /**
         * Retrieve an int value and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static int getInt(@NonNull String key, int defValue) {
            return sSharedPreferences.getInt(key, defValue);
        }

        /**
         * Store an int value.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static void putInt(@NonNull String key, int value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putInt(key, value);
            editor.apply();
        }

        /**
         * Retrieve a long value.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or 0L if key is not set.
         */
        @SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue"})
        public static long getLong(@NonNull String key) {
            return getLong(key, 0L);
        }

        /**
         * Retrieve a long value and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static long getLong(@NonNull String key, long defValue) {
            return sSharedPreferences.getLong(key, defValue);
        }

        /**
         * Store a long value.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public static void putLong(@NonNull String key, long value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putLong(key, value);
            editor.apply();
        }

        /**
         * Retrieve a string value.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or {@code null} if key is not set.
         */
        @SuppressWarnings("unused")
        public static String getString(@NonNull String key) {
            return getString(key, null);
        }

        /**
         * Retrieve a string value and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        public static String getString(@NonNull String key, String defValue) {
            return sSharedPreferences.getString(key, defValue);
        }

        /**
         * Store a string value.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static void putString(@NonNull String key, String value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

        /**
         * Retrieve a string set.
         *
         * @param key The key for which the value is to be retrieved.
         * @return The value of {@code key} or {@code null} if key is not set.
         */
        @SuppressWarnings("unused")
        public static Set<String> getStringSet(@NonNull String key) {
            return getStringSet(key, null);
        }

        /**
         * Retrieve a string set and provide a default value.
         *
         * @param key      The key for which the value is to be retrieved.
         * @param defValue The default value to return if no value is set for {@code key}.
         * @return The value of {@code key} or the default value if key is not set.
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public static Set<String> getStringSet(@NonNull String key, Set<String> defValue) {
            return sSharedPreferences.getStringSet(key, defValue);
        }

        /**
         * Store a string set.
         *
         * @param key   The key to store the value for.
         * @param value The value to store for the key.
         */
        @SuppressWarnings("unused")
        public static void putStringSet(@NonNull String key, Set<String> value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putStringSet(key, value);
            editor.apply();
        }

        /**
         * Removes a value with the given key.
         *
         * @param key Key of the value to be removed.
         */
        public static void remove(@NonNull String key) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }

        /**
         * Removes all keys and values.
         */
        public static void clear() {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }

    /**
     * InternalStorage Helper class
     */
    public static class InternalStorage {

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
                    while ((line = reader.readLine()) != null) {
                        contents.append(line).append(lineSeparator);
                    }
                } finally {

                    //noinspection ThrowFromFinallyBlock
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
            byte fileContents[] = new byte[(int) file.length()];
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
                //noinspection ThrowFromFinallyBlock
                writer.close();
            }
        }

        /**
         * Read an object from a file (deserialization).
         *
         * @param file The file to read from.
         * @param <T>  A type for the deserialized instance.
         * @return The deserialized instance.
         * @throws IOException            If an I/O error occurs
         * @throws ClassNotFoundException If no class definition found for serialized instance.
         */
        @SuppressWarnings("unchecked")
        public static <T extends Serializable> T readObject(@NonNull File file)
                throws IOException, ClassNotFoundException {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
            //noinspection TryFinallyCanBeTryWithResources
            try {
                return (T) inputStream.readObject();
            } finally {
                //noinspection ThrowFromFinallyBlock
                inputStream.close();
            }
        }

        /**
         * Write an object to a file (serialization).
         *
         * @param file   The file to write to.
         * @param object The object to be written to the file.
         * @param <T>    A type for the object.
         * @throws IOException If an I/O error occurs
         */
        public static <T extends Serializable> void writeObject(@NonNull File file, @NonNull T object) throws IOException {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));

            //noinspection TryFinallyCanBeTryWithResources
            try {
                outputStream.writeObject(object);
            } finally {

                //noinspection ThrowFromFinallyBlock
                outputStream.close();
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
         * Create a directory if it does not already exist.
         * Will create the whole directory tree if necessary.
         *
         * @param path An absolute path for the directory to be created.
         */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "SpellCheckingInspection"})
        public static void mkdir(@NonNull String path) {
            new File(path).mkdirs();
        }
    }

    /**
     * DatabaseStorage Helper class
     */
    public static class DatabaseStorage implements Closeable {

        /**
         * DatabaseManager instance.
         */
        private final DatabaseManager mDatabaseManager;

        /**
         * Private constructor.
         *
         * @param databaseManager An instance of {@code DatabaseManager}.
         */
        private DatabaseStorage(@NonNull DatabaseManager databaseManager) {
            mDatabaseManager = databaseManager;
        }

        /**
         * Get a new instance of {@code DatabaseManager}.
         *
         * @param database The database name.
         * @param table    The table name.
         * @param version  The version.
         * @param schema   The schema of the database. If the database has more than one table,
         *                 it should contain schemas for all the tables.
         * @param listener The database listener.
         * @return database storage.
         */
        @SuppressWarnings("WeakerAccess")
        public static DatabaseStorage getDatabaseStorage(@NonNull String database,
                                                         @NonNull String table,
                                                         @IntRange(from = 1) int version,
                                                         @NonNull ContentValues schema,
                                                         @NonNull DatabaseManager.Listener listener) {
            return new DatabaseStorage(new DatabaseManager(sContext, database, table, version, schema, listener));
        }

        /**
         * Set maximum SQLite database size.
         *
         * @param maxStorageSizeInBytes Maximum SQLite database size.
         */
        public boolean setMaxStorageSize(long maxStorageSizeInBytes) {
            return mDatabaseManager.setMaxSize(maxStorageSizeInBytes);
        }

        /**
         * Store an entry in a table.
         *
         * @param values The entry to be stored.
         * @return The identifier of the created database entry.
         */
        public long put(@NonNull ContentValues values) {
            return mDatabaseManager.put(values);
        }

        /**
         * Delete an entry in a table.
         *
         * @param id The identifier for the entry to be deleted.
         */
        public void delete(@IntRange(from = 0) long id) {
            mDatabaseManager.delete(id);
        }

        /**
         * Deletes the entries by the identifier from the database.
         *
         * @param idList The list of database identifiers.
         */
        public void delete(@NonNull List<Long> idList) {
            mDatabaseManager.delete(idList);
        }

        /**
         * Deletes the entries that matches key == value.
         *
         * @param key   The optional key for query.
         * @param value The optional value for query.
         */
        public void delete(@Nullable String key, @Nullable Object value) {
            mDatabaseManager.delete(key, value);
        }

        /**
         * Gets the entry by the identifier.
         *
         * @param id The database identifier.
         * @return An entry for the identifier or null if not found.
         */
        public ContentValues get(@IntRange(from = 0) long id) {
            return mDatabaseManager.get(id);
        }

        /**
         * Gets the entry that matches key == value.
         *
         * @param key   The optional key for query.
         * @param value The optional value for query.
         * @return A matching entry.
         */
        public ContentValues get(@Nullable String key, @Nullable Object value) {
            return mDatabaseManager.get(key, value);
        }

        /**
         * Gets a scanner to iterate all values.
         *
         * @return A scanner to iterate all values.
         */
        @SuppressWarnings("WeakerAccess")
        public DatabaseScanner getScanner() {
            return getScanner(null, null);
        }

        /**
         * Gets a scanner to iterate all values those match key == value.
         *
         * @param key   The optional key for query.
         * @param value The optional value for query.
         * @return A scanner to iterate all values.
         */
        public DatabaseScanner getScanner(@Nullable String key, @Nullable Object value) {
            return getScanner(key, value, null, null, false);
        }

        /**
         * Gets a scanner to iterate all values those match key == value, but records contain
         * only identifiers.
         *
         * @param key          The optional key1 for query.
         * @param value        The optional value1 for query.
         * @param key2         The optional key2 for query.
         * @param value2Filter The optional values to exclude from query that matches key2.
         * @param idOnly       True to return only identifiers, false to return all fields.
         *                     This flag is ignored if using in memory database.
         * @return A scanner to iterate all values (records contain only identifiers).
         */
        public DatabaseScanner getScanner(@Nullable String key, @Nullable Object value, String key2, Collection<String> value2Filter, boolean idOnly) {
            return new DatabaseScanner(mDatabaseManager.getScanner(key, value, key2, value2Filter, idOnly));
        }

        /**
         * Clears the table in the database.
         */
        public void clear() {
            mDatabaseManager.clear();
        }

        /**
         * Closes database and cleans up in-memory database.
         */
        @Override
        public void close() {
            mDatabaseManager.close();
        }

        /**
         * Gets the count of records in the table.
         *
         * @return The number of records in the table.
         */
        public long size() {
            return mDatabaseManager.getRowCount();
        }

        /**
         * Gets the maximum size of the database.
         *
         * @return The maximum size of database in bytes.
         */
        @SuppressWarnings("unused")
        public long getMaxSize() {
            return mDatabaseManager.getMaxSize();
        }

        /**
         * Gets an array of column names in the table.
         *
         * @return An array of column names.
         */
        @VisibleForTesting
        String[] getColumnNames() {
            return mDatabaseManager.getCursor(null, null, null, null, false).getColumnNames();
        }

        /**
         * Database scanner to iterate over values.
         */
        public static class DatabaseScanner implements Iterable<ContentValues>, Closeable {

            private final DatabaseManager.Scanner mScanner;

            private DatabaseScanner(DatabaseManager.Scanner scanner) {
                mScanner = scanner;
            }

            @Override
            public void close() {
                mScanner.close();
            }

            @NonNull
            @Override
            public Iterator<ContentValues> iterator() {
                return mScanner.iterator();
            }

            public int getCount() {
                return mScanner.getCount();
            }
        }
    }
}