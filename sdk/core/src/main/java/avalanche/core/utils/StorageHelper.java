package avalanche.core.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * StorageHelper class to access local storage.
 */
public final class StorageHelper {

    /**
     * Name of preferences.
     */
    private static final String PREFERENCES_NAME = "AvalancheSDK";

    /**
     * Application context instance.
     */
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
    public static void initialize(Context context) {
        sContext = context;
        sSharedPreferences = sContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * PreferencesStorage Helper class
     */
    public final static class PreferencesStorage {
        /*
         * boolean value
         */
        public static boolean getBoolean(@NonNull String key) {
            return getBoolean(key, false);
        }

        public static boolean getBoolean(@NonNull String key, boolean defValue) {
            return sSharedPreferences.getBoolean(key, defValue);
        }

        public static void putBoolean(@NonNull String key, boolean value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }

        /*
         * float value
         */
        public static float getFloat(@NonNull String key) {
            return getFloat(key, 0f);
        }

        public static float getFloat(@NonNull String key, float defValue) {
            return sSharedPreferences.getFloat(key, defValue);
        }

        public static void putFloat(@NonNull String key, float value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putFloat(key, value);
            editor.apply();
        }

        /*
         * int value
         */
        public static int getInt(@NonNull String key) {
            return getInt(key, 0);
        }

        public static int getInt(@NonNull String key, int defValue) {
            return sSharedPreferences.getInt(key, defValue);
        }

        public static void putInt(@NonNull String key, int value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putInt(key, value);
            editor.apply();
        }

        /*
         * long value
         */
        public static long getLong(@NonNull String key) {
            return getLong(key, 0L);
        }

        public static long getLong(@NonNull String key, long defValue) {
            return sSharedPreferences.getLong(key, defValue);
        }

        public static void putLong(@NonNull String key, long value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putLong(key, value);
            editor.apply();
        }

        /*
         * String value
         */
        public static String getString(@NonNull String key) {
            return getString(key, null);
        }

        public static String getString(@NonNull String key, String defValue) {
            return sSharedPreferences.getString(key, defValue);
        }

        public static void putString(@NonNull String key, String value) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

        /*
         * Set<String> value
         */
        public static Set<String> getStringSet(@NonNull String key) {
            return getStringSet(key, null);
        }

        public static Set<String> getStringSet(@NonNull String key, Set<String> defValue) {
            return sSharedPreferences.getStringSet(key, defValue);
        }

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
    }

    /**
     * InternalStorage Helper class
     */
    public final static class InternalStorage {
        /**
         * Reads contents from the file.
         *
         * @param filename The name of the file.
         * @return The contents of the file.
         */
        public static String read(@NonNull String filename) {
            return read(new File(filename));
        }

        /**
         * Reads contents from the file.
         *
         * @param file The file instance.
         * @return The contents of the file.
         */
        public static String read(@NonNull File file) {
            StringBuilder contents = new StringBuilder();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                String lineSeparator = System.getProperty("line.separator");
                while ((line = reader.readLine()) != null) {
                    contents.append(line).append(lineSeparator);
                }
            } catch (FileNotFoundException ignored) {
                // Log the exception and return an empty string.
                AvalancheLog.error("Cannot find file " + file.getAbsolutePath());
            } catch (IOException ignored) {
                // Ignore IOException and return the already read contents.
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            return contents.toString();
        }

        /**
         * Writes contents to the file.
         *
         * @param filename The name of the file.
         * @param contents The content to be written to the file.
         * @throws IOException
         */
        public static void write(@NonNull String filename, String contents) throws IOException {
            write(new File(filename), contents);
        }

        /**
         * Writes contents to the file.
         *
         * @param file     The file instance.
         * @param contents The content to be written to the file.
         * @throws IOException
         */
        public static void write(@NonNull File file, String contents) throws IOException {
            if (TextUtils.isEmpty(contents) || TextUtils.getTrimmedLength(contents) <= 0) {
                return;
            }

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file));
                writer.write(contents);
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        /**
         * Reads an object from the file (deserialization).
         *
         * @param file The file instance.
         * @return The deserialized instance.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        @SuppressWarnings("unchecked")
        public static <T extends Serializable> T readObject(@NonNull File file)
                throws IOException, ClassNotFoundException {
            ObjectInputStream inputStream = null;
            T object;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(file));
                object = (T) inputStream.readObject();
            } finally {
                if (inputStream != null)
                    inputStream.close();
            }
            return object;
        }

        /**
         * Writes and object to the file (serialization).
         *
         * @param file   The file instance.
         * @param object The object to be written to the file.
         * @throws IOException
         */
        public static <T extends Serializable> void writeObject(@NonNull File file, T object) throws IOException {
            ObjectOutputStream outputStream = null;
            try {
                outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(object);
            } finally {
                if (outputStream != null)
                    outputStream.close();
            }
        }

        /**
         * Gets an array of filenames in the path.
         *
         * @param path   The directory path.
         * @param filter The filter to match names against, may be {@code null}.
         * @return An array of filename that doesn't include paths.
         */
        public static String[] getFilenames(@NonNull String path, FilenameFilter filter) {
            File dir = new File(path);
            if (dir.exists()) {
                return dir.list(filter);
            }

            return new String[0];
        }

        /**
         * Gets the most recent file in the path.
         *
         * @param path   The directory path.
         * @param filter The filter to match names against, may be {@code null}.
         * @return A file instance, may be {@code null} if the directory is empty.
         */
        public static File lastModifiedFile(@NonNull String path, FilenameFilter filter) {
            File dir = new File(path);
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
         * Deletes a file or directory with the given name.
         *
         * @param name The name of the file or directory.
         * @return {@code true} if it was deleted, {@code false} otherwise.
         */
        public static boolean delete(@NonNull String name) {
            return delete(new File(name));
        }

        /**
         * Deletes the file.
         *
         * @param file The file instance.
         * @return {@code true} if it was deleted, {@code false} otherwise.
         */
        public static boolean delete(@NonNull File file) {
            return file.delete();
        }

        /**
         * Creates the directory if it does not already exist.
         *
         * @param path An absolute path for directory.
         */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "SpellCheckingInspection"})
        public static void mkdir(@NonNull String path) {
            File dir = new File(path);
            dir.mkdirs();
        }
    }

    /**
     * DatabaseStorage Helper class
     */
    public final static class DatabaseStorage implements Closeable {

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
         * Gets a new instance of {@code DatabaseManager}.
         *
         * @param database The database name.
         * @param table    The table name.
         * @param version  The version.
         * @param schema   The schema of the database. If the database has tables more than 1,
         *                 the schema should contain schemas for all the tables.
         * @param listener The error listener
         * @return database storage.
         */
        public static DatabaseStorage getDatabaseStorage(@NonNull String database,
                                                         @NonNull String table,
                                                         @IntRange(from = 1) int version,
                                                         @NonNull ContentValues schema,
                                                         final DatabaseErrorListener listener) {
            return getDatabaseStorage(database, table, version, schema, 0, listener);
        }

        /**
         * Gets a new instance of {@code DatabaseManager}.
         *
         * @param database   The database name.
         * @param table      The table name.
         * @param version    The version.
         * @param schema     The schema of the database. If the database has tables more than 1,
         *                   the schema should contain schemas for all the tables.
         * @param maxRecords The maximum number of records allowed in the table.
         * @param listener   The error listener
         * @return database storage.
         */
        public static DatabaseStorage getDatabaseStorage(@NonNull String database,
                                                         @NonNull String table,
                                                         @IntRange(from = 1) int version,
                                                         @NonNull ContentValues schema,
                                                         @IntRange(from = 0) int maxRecords,
                                                         final DatabaseErrorListener listener) {
            return new DatabaseStorage(new DatabaseManager(sContext, database, table, version, schema, maxRecords, new DatabaseManager.ErrorListener() {
                @Override
                public void onError(String operation, RuntimeException e) {
                    listener.onError(operation, e);
                }
            }));
        }

        /**
         * Stores the entry to the table.
         *
         * @param values The entry to be stored.
         * @return A database identifier
         */
        public Long put(@NonNull ContentValues values) {
            return mDatabaseManager.put(values);
        }

        /**
         * Updates the entry for the identifier.
         *
         * @param id     The existing database identifier.
         * @param values The entry to be updated.
         * @return true if the values updated successfully, false otherwise.
         */
        public boolean update(@IntRange(from = 0) long id, @NonNull ContentValues values) {
            return mDatabaseManager.update(id, values);
        }

        /**
         * Deletes the entry by the identifier from the database.
         *
         * @param id The database identifier.
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
            return new DatabaseScanner(mDatabaseManager.getScanner(key, value));
        }

        /**
         * Clears the table in the database.
         */
        public void clear() {
            mDatabaseManager.clear();
        }

        /**
         * Closes database and clean up in-memory database.
         *
         * @throws IOException
         */
        @Override
        public void close() throws IOException {
            mDatabaseManager.close();
        }

        /**
         * Gets the count of records in the table.
         *
         * @return The number of records in the table.
         */
        @VisibleForTesting
        public long getRowCount() {
            return mDatabaseManager.getRowCount();
        }

        /**
         * Gets an array of column names in the table.
         *
         * @return An array of column names.
         */
        @VisibleForTesting
        String[] getColumnNames() {
            return mDatabaseManager.getCursor(null, null).getColumnNames();
        }

        /**
         * Listener specification, each callback is called only once per instance
         */
        public interface DatabaseErrorListener {
            /**
             * Notifies an exception
             */
            void onError(String operation, RuntimeException e);
        }

        /**
         * Database scanner to iterate values.
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

            @Override
            public Iterator<ContentValues> iterator() {
                return mScanner.iterator();
            }
        }
    }
}