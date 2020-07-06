/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unused")
public class SQLiteUtilsAndroidTest {

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Database schema.
     */
    private static ContentValues sSchema;

    /**
     * Test database name.
     */
    private static final String DATABASE_NAME = "test-database";

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();

        /* Create a test schema. */
        sSchema = generateContentValues();
    }

    @After
    public void tearDown() {
        sContext.deleteDatabase(DATABASE_NAME);
    }

    private static ContentValues generateContentValues() {
        ContentValues values = new ContentValues();
        values.put("COL_STRING", "some_column_string");
        return values;
    }

    @Test
    public void test() {
        new SQLiteUtils();
        assertNotNull(SQLiteUtils.newSQLiteQueryBuilder());
    }

    @Test
    public void testTableMethods() {
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "test.setMaximumSize", 1, sSchema, mock(DatabaseManager.Listener.class));
        String tableName = "someTableName";
        String columnName = "someColumnName";
        String objectForColumnStringType = "someTextType";
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, objectForColumnStringType);
        SQLiteUtils.createTable(databaseManager.getDatabase(), tableName, contentValues);
        assertTrue(checkTableExists(databaseManager, tableName));
        Cursor cursor = databaseManager.getCursor(tableName, SQLiteUtils.newSQLiteQueryBuilder(), new String[]{columnName}, null, null);
        if (cursor != null) {
            try {
                assertTrue(cursor.getColumnCount() > 0);
            } finally {
                cursor.close();
            }
        }
        SQLiteUtils.dropTable(databaseManager.getDatabase(), tableName);
        assertFalse(checkTableExists(databaseManager, tableName));
    }

    private boolean checkTableExists(DatabaseManager databaseManager, String tableName) {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere("tbl_name = ?");
        Cursor cursor = databaseManager.getCursor("sqlite_master", builder, new String[]{"tbl_name"}, new String[]{tableName}, null);
        if (cursor != null) {
            try {
                return cursor.getCount() > 0;
            } finally {
                cursor.close();
            }
        }
        return false;
    }
}
