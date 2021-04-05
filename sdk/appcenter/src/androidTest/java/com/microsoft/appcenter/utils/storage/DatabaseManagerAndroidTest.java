/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
@SmallTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class DatabaseManagerAndroidTest {

    /**
     * Random tool.
     */
    private static final Random RANDOM = new Random();

    /**
     * Initial maximum database size for some of the tests.
     */
    private static final long MAX_SIZE_IN_BYTES = 20480;

    /**
     * Test database name.
     */
    private static final String DATABASE_NAME = "test-database";

    /**
     * Test database creation command.
     */
    private static final String CREATE_TEST_SQL = "CREATE TABLE IF NOT EXISTS `databaseManager`" +
            " (`oid` INTEGER PRIMARY KEY AUTOINCREMENT," +
            "`COL_BYTE` INTEGER, `COL_SHORT` INTEGER," +
            "`COL_LONG` INTEGER," +
            "`COL_BOOLEAN` INTEGER," +
            "`COL_BYTE_ARRAY` BLOB," +
            "`COL_FLOAT` REAL," +
            "`COL_STRING` TEXT," +
            "`COL_STRING_NULL` TEXT," +
            "`COL_INTEGER` INTEGER," +
            "`COL_DOUBLE` REAL)";

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    /**
     * Database schema.
     */
    private static ContentValues mSchema;

    /**
     * Boolean value to simulate both true and false.
     */
    private static boolean mRandomBooleanValue = false;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getInstrumentation().getContext();

        /* Create a test schema. */
        mSchema = generateContentValues();
    }

    @After
    public void tearDown() {
        sContext.deleteDatabase(DATABASE_NAME);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private static ContentValues get(DatabaseManager databaseManager, long id) {
        try {
            SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
            builder.appendWhere(DatabaseManager.PRIMARY_KEY + " = ?");
            String[] selectionArgs = new String[]{String.valueOf(id)};
            Cursor cursor = databaseManager.getCursor(builder, null, selectionArgs, null);
            try {
                return databaseManager.nextValues(cursor);
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void runDatabaseManagerTest(DatabaseManager databaseManager) {
        ContentValues value1 = generateContentValues();
        ContentValues value2 = generateContentValues();
        ContentValues value3 = generateContentValues();

        /* Put. */
        long value1Id = databaseManager.put(value1, "COL_INTEGER");
        assertTrue(value1Id >= 0);

        /* Put another. */
        long value2Id = databaseManager.put(value2, "COL_INTEGER");
        assertTrue(value2Id >= 0);

        /* Generate an ID that is neither value1Id nor value2Id. */

        /* Get. */
        ContentValues value1FromDatabase = get(databaseManager, value1Id);
        assertContentValuesEquals(value1, value1FromDatabase);
        ContentValues value2FromDatabase = get(databaseManager, value2Id);
        assertContentValuesEquals(value2, value2FromDatabase);
        //noinspection ResourceType
        ContentValues nullValueFromDatabase = get(databaseManager, -1);
        assertNull(nullValueFromDatabase);

        /* Count with scanner. */
        Cursor cursor = databaseManager.getCursor(null, null, null, null);
        assertEquals(2, cursor.getCount());
        assertEquals(2, cursor.getCount());
        SQLiteQueryBuilder queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING = ?");
        Cursor cursor1 = databaseManager.getCursor(queryBuilder, null, new String[]{value1.getAsString("COL_STRING")}, null);
        assertEquals(1, cursor1.getCount());
        assertTrue(cursor1.moveToNext());
        assertContentValuesEquals(value1, databaseManager.buildValues(cursor1));
        assertFalse(cursor1.moveToNext());

        /* Null value matching. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING IS NULL");
        assertEquals(0, databaseManager.getCursor(queryBuilder, null, null, null).getCount());
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING_NULL IS NULL");
        assertEquals(2, databaseManager.getCursor(queryBuilder, null, null, null).getCount());

        /* Test null value filter does not exclude anything, so returns the 2 logs. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        cursor = databaseManager.getCursor(queryBuilder, null, null, null);
        assertEquals(2, cursor.getCount());

        /* Test filtering only with the second key parameter to get only the second log. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING NOT IN (?)");
        cursor = databaseManager.getCursor(queryBuilder, null, new String[]{value1.getAsString("COL_STRING")}, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertContentValuesEquals(value2, databaseManager.buildValues(cursor));

        /* Delete. */
        databaseManager.delete(value1Id);
        assertNull(get(databaseManager, value1Id));
        assertEquals(1, databaseManager.getRowCount());
        assertEquals(1, databaseManager.getCursor(null, null, null, null).getCount());

        /* Put logs to delete with condition. */
        ContentValues value4 = generateContentValues();
        ContentValues value5 = generateContentValues();
        value4.put("COL_STRING", value2.getAsString("COL_STRING"));
        value5.put("COL_STRING", value2.getAsString("COL_STRING") + "A");
        long value4Id = databaseManager.put(value4, "COL_INTEGER");
        long value5Id = databaseManager.put(value5, "COL_INTEGER");
        assertTrue(value4Id >= 0);
        assertTrue(value5Id >= 0);

        /* Delete logs with condition. */
        databaseManager.delete("COL_STRING", value2.getAsString("COL_STRING"));
        assertEquals(1, databaseManager.getRowCount());
        ContentValues value5FromDatabase = get(databaseManager, value5Id);
        assertContentValuesEquals(value5, value5FromDatabase);

        /* Clear. */
        databaseManager.clear();
        assertEquals(0, databaseManager.getRowCount());
    }

    private static ContentValues generateContentValues() {
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        ContentValues values = new ContentValues();
        values.put("COL_STRING", new String(randomBytes));
        values.put("COL_STRING_NULL", (String) null);
        values.put("COL_BYTE", randomBytes[0]);
        values.put("COL_SHORT", (short) RANDOM.nextInt(100));
        values.put("COL_INTEGER", RANDOM.nextInt());
        values.put("COL_LONG", RANDOM.nextLong());
        values.put("COL_FLOAT", RANDOM.nextFloat());
        values.put("COL_DOUBLE", RANDOM.nextDouble());
        values.put("COL_BOOLEAN", (mRandomBooleanValue = !mRandomBooleanValue)/*RANDOM.nextBoolean()*/);
        values.put("COL_BYTE_ARRAY", randomBytes);
        return values;
    }

    private static void assertContentValuesEquals(ContentValues expected, ContentValues actual) {
        assertNotNull(actual);
        assertEquals(expected.getAsString("COL_STRING"), actual.getAsString("COL_STRING"));
        assertEquals(expected.getAsString("COL_STRING_NULL"), actual.getAsString("COL_STRING_NULL"));
        assertEquals(expected.getAsByte("COL_BYTE"), actual.getAsByte("COL_BYTE"));
        assertEquals(expected.getAsShort("COL_SHORT"), actual.getAsShort("COL_SHORT"));
        assertEquals(expected.getAsInteger("COL_INTEGER"), actual.getAsInteger("COL_INTEGER"));
        assertEquals(expected.getAsLong("COL_LONG"), actual.getAsLong("COL_LONG"));
        assertEquals(expected.getAsFloat("COL_FLOAT"), actual.getAsFloat("COL_FLOAT"));
        assertEquals(expected.getAsDouble("COL_DOUBLE"), actual.getAsDouble("COL_DOUBLE"));
        assertEquals(expected.getAsBoolean("COL_BOOLEAN"), actual.getAsBoolean("COL_BOOLEAN"));
        assertArrayEquals(expected.getAsByteArray("COL_BYTE_ARRAY"), actual.getAsByteArray("COL_BYTE_ARRAY"));
    }

    @Test
    public void databaseManager() {

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManager", 1, mSchema, CREATE_TEST_SQL, listener);

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            runDatabaseManagerTest(databaseManager);
        } finally {

            /* Close. */
            databaseManager.close();
        }
        verify(listener).onCreate(any(SQLiteDatabase.class));
    }

    @Test
    public void setMaximumSize() {

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManager", 1, mSchema, CREATE_TEST_SQL, mock(DatabaseManager.Listener.class));

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {

            /* Test to change to an exact size as its multiple of 4KB. */
            assertTrue(databaseManager.setMaxSize(MAX_SIZE_IN_BYTES));
            assertEquals(MAX_SIZE_IN_BYTES, databaseManager.getMaxSize());

            /* Test inexact value, it will use next multiple of 4KB. */
            long desiredSize = MAX_SIZE_IN_BYTES * 2 + 1;
            assertTrue(databaseManager.setMaxSize(desiredSize));
            assertEquals(desiredSize - 1 + 4096, databaseManager.getMaxSize());

            /* Try to set to a very small value. */
            assertFalse(databaseManager.setMaxSize(2));

            /* Test the side effect is that we shrunk to the minimum size that is possible. */
            assertEquals(MAX_SIZE_IN_BYTES, databaseManager.getMaxSize());
        } finally {

            /* Close. */
            databaseManager.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private boolean checkTableExists(DatabaseManager databaseManager, String tableName) {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere("tbl_name = ?");
        Cursor cursor = databaseManager.getCursor("sqlite_master", builder, new String[]{"tbl_name"}, new String[]{tableName}, null);
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }
}
