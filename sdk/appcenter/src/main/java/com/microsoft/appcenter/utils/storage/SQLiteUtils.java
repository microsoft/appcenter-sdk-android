/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import java.util.Map;

public class SQLiteUtils {

    private static final String CREATE_LOGS_SQL = "CREATE TABLE IF NOT EXISTS `logs` (oid INTEGER PRIMARY KEY AUTOINCREMENT," +
            " `target_token` TEXT, `type` TEXT, `priority` INTEGER, `log` TEXT, `persistence_group` TEXT, `target_key` TEXT);";
    private static final String DROP_LOGS_SQL = "DROP TABLE `logs`";
    private static final String CREATE_PRIORITY_INDEX_LOGS = "CREATE INDEX `ix_logs_priority` ON logs (`priority`)";

    @NonNull
    public static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }

    public static void createLogsTable(SQLiteDatabase db) {
        SQLiteStatement stmt = db.compileStatement(CREATE_LOGS_SQL);
        stmt.execute();
    }

    public static void dropLogsTable(@NonNull SQLiteDatabase db) {
        SQLiteStatement stmt = db.compileStatement(DROP_LOGS_SQL);
        stmt.execute();
    }

    public static void createPriorityIndex(SQLiteDatabase db) {
        SQLiteStatement stmt = db.compileStatement(CREATE_PRIORITY_INDEX_LOGS);
        stmt.execute();
    }
}