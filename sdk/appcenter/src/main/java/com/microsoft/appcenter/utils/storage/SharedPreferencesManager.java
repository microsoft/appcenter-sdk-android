/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Set;

/**
 * Shared preferences manager.
 */
public class SharedPreferencesManager {

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
     * Initializes SharedPreferencesManager class.
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
    @Nullable
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
