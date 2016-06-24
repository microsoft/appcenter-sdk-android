package avalanche.base.utils;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.Set;

/**
 * StorageHelper class to access local storage.
 */
public final class StorageHelper {
    /**
     * Android SharedPreferences instance.
     */
    private static android.content.SharedPreferences sSharedPreferences;

    /**
     * Initializes StorageHelper class.
     *
     * @param context The context of the application.
     */
    public static void initialize(Context context) {
        sSharedPreferences = context.getSharedPreferences("AvalancheSDK", Context.MODE_PRIVATE);
    }

    /**
     * SharesPreferences Helper class
     */
    public final static class SharedPreferences {
        /*
         * boolean value
         */
        public static boolean getBoolean(String key) {
            return getBoolean(key, false);
        }

        public static boolean getBoolean(String key, boolean defValue) {
            return sSharedPreferences.getBoolean(key, defValue);
        }

        public static void putBoolean(String key, boolean value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }

        /*
         * float value
         */
        public static float getFloat(String key) {
            return getFloat(key, 0f);
        }

        public static float getFloat(String key, float defValue) {
            return sSharedPreferences.getFloat(key, defValue);
        }

        public static void putFloat(String key, float value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putFloat(key, value);
            editor.apply();
        }

        /*
         * int value
         */
        public static int getInt(String key) {
            return getInt(key, 0);
        }

        public static int getInt(String key, int defValue) {
            return sSharedPreferences.getInt(key, defValue);
        }

        public static void putInt(String key, int value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putInt(key, value);
            editor.apply();
        }

        /*
         * long value
         */
        public static long getLong(String key) {
            return getLong(key, 0l);
        }

        public static long getLong(String key, long defValue) {
            return sSharedPreferences.getLong(key, defValue);
        }

        public static void putLong(String key, long value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putLong(key, value);
            editor.apply();
        }

        /*
         * String value
         */
        public static String getString(String key) {
            return getString(key, null);
        }

        public static String getString(String key, String defValue) {
            return sSharedPreferences.getString(key, defValue);
        }

        public static void putString(String key, String value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

        /*
         * Set<String> value
         */
        public static Set<String> getStringSet(String key) {
            return getStringSet(key, null);
        }

        public static Set<String> getStringSet(String key, Set<String> defValue) {
            return sSharedPreferences.getStringSet(key, defValue);
        }

        public static void putStringSet(String key, Set<String> value) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.putStringSet(key, value);
            editor.apply();
        }

        /**
         * Removes a value with the given key.
         *
         * @param key A key.
         */
        public static void remove(String key) {
            android.content.SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }
    }

    public static class InternalStorage {
        /**
         * Reads contents from the file.
         *
         * @param filename The name of the file.
         * @return The contents of the file.
         */
        public static String read(String filename) {
            return read(new File(filename));
        }

        /**
         * Reads contents from the file.
         *
         * @param file The file instance.
         * @return The contents of the file.
         */
        public static String read(File file) {
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
                // Log the exception and return empty string.
                AvalancheLog.error("Cannot find file " + file.getAbsolutePath());
            } catch (IOException ignored) {
                // Ignore IOException and return empty string.
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
        public static void write(String filename, String contents) throws IOException {
            write(new File(filename), contents);
        }

        /**
         * Writes contents to the file.
         *
         * @param file     The file instance.
         * @param contents The content to be written to the file.
         * @throws IOException
         */
        public static void write(File file, String contents) throws IOException {
            if (TextUtils.isEmpty(contents)) {
                return;
            }

            BufferedWriter writer = null;
            try {
                if (!TextUtils.isEmpty(contents) && TextUtils.getTrimmedLength(contents) > 0) {
                    writer = new BufferedWriter(new FileWriter(file));
                    writer.write(contents);
                    writer.flush();
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        /**
         * Reads an object from the file (deserialization).
         *
         * @param file  The file instance.
         * @param clazz The type of the object to be deserialized.
         * @return The deserialized instance.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        public static <T extends Serializable> T readObject(File file, Class<T> clazz) throws IOException, ClassNotFoundException {
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
        public static <T extends Serializable> void writeObject(File file, T object) throws IOException {
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
        public static String[] getFilenames(String path, FilenameFilter filter) {
            File dir = new File(path);
            if (dir.exists()) {
                return dir.list(filter);
            }

            return new String[0];
        }

        /**
         * Gets the most recent file in the path.
         * @param path The directory path.
         * @param filter The filter to match names against, may be {@code null}.
         * @return A file instance, may be {@code null} if the directory is empty.
         */
        public static File lastModifiedFile(String path, FilenameFilter filter) {
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
         * Deletes a file with the given filename.
         *
         * @param filename The name of the file.
         * @return {@code true} if this file was deleted, {@code false} otherwise.
         */
        public static boolean delete(String filename) {
            return delete(new File(filename));
        }

        /**
         * Deletes the file.
         *
         * @param file The file instance.
         * @return {@code true} if this file was deleted, {@code false} otherwise.
         */
        public static boolean delete(File file) {
            return file.delete();
        }
    }
}