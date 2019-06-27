/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;

public class SQLiteUtils {

    @NonNull
    public static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }

    public static void dropTable(@NonNull SQLiteDatabase db, @NonNull String table) {
        db.execSQL(formatDropTableQuery(table));
    }

    @NonNull
    public static String formatDropTableQuery(@NonNull String table) {
        return String.format("DROP TABLE `%s`", table);
    }
}