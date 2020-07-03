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

    @NonNull
    public static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }

    public static void createTable(SQLiteDatabase db, String table, ContentValues schema) {

        /* Generate a schema from specimen. */
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `");
        sql.append(table);
        sql.append("` (oid INTEGER PRIMARY KEY AUTOINCREMENT");
        for (Map.Entry<String, Object> col : schema.valueSet()) {
            sql.append(", `").append(col.getKey()).append("` ");
            Object val = col.getValue();
            if (val instanceof Double || val instanceof Float) {
                sql.append("REAL");
            } else if (val instanceof Number || val instanceof Boolean) {
                sql.append("INTEGER");
            } else if (val instanceof byte[]) {
                sql.append("BLOB");
            } else {
                sql.append("TEXT");
            }
        }
        sql.append(");");
        SQLiteStatement statement = db.compileStatement(sql.toString());
        statement.execute();
    }

    public static void dropTable(@NonNull SQLiteDatabase db, @NonNull String table) {
        SQLiteStatement statement = db.compileStatement(String.format("DROP TABLE `%s`", table));
        statement.execute();
    }
}