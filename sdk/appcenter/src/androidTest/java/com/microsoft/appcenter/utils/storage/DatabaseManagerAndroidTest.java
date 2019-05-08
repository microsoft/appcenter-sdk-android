/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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
@RunWith(AndroidJUnit4.class)
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
        sContext = InstrumentationRegistry.getTargetContext();

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
        Long value4Id = databaseManager.put(value4, "COL_INTEGER");
        Long value5Id = databaseManager.put(value5, "COL_INTEGER");
        assertNotNull(value4Id);
        assertNotNull(value5Id);

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
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManager", 1, mSchema, listener);

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
    public void databaseManagerWithZeroUniqueConstraint() {

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManager", 1, mSchema, listener, new String[0]);

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
    public void databaseManagerUpgradeNotHandled() {

        /* Create a schema for v1. */
        ContentValues schema = new ContentValues();
        schema.put("COL_STRING", "");

        /* Create a row for v1. */
        ContentValues oldVersionValue = new ContentValues();
        oldVersionValue.put("COL_STRING", "Hello World");

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManagerUpgrade", 1, schema, new DatabaseManager.DefaultListener());
        try {

            /* Database will always create a column for identifiers so default length of all tables is 1. */
            Cursor cursor = databaseManager.getCursor(SQLiteUtils.newSQLiteQueryBuilder(), null, null, null);
            assertEquals(2, cursor.getColumnCount());
            long id = databaseManager.put(oldVersionValue, "COL_INTEGER");

            /* Put data. */
            ContentValues actual = get(databaseManager, id);
            assertNotNull(actual);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }

        /* Get instance to access database with a newer schema without handling upgrade. */
        databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManagerUpgrade", 2, mSchema, new DatabaseManager.DefaultListener());

        /* Verify data deleted since no handled upgrade. */
        try {
            Cursor cursor = databaseManager.getCursor(SQLiteUtils.newSQLiteQueryBuilder(), null, null, null);
            assertEquals(11, cursor.getColumnCount());
            assertEquals(0, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }
    }

    @Test
    public void databaseManagerUpgradeHandled() {

        /* Create a schema for v1. */
        ContentValues schema = new ContentValues();
        schema.put("COL_STRING", "");

        /* Create a row for v1. */
        ContentValues oldVersionValue = new ContentValues();
        oldVersionValue.put("COL_STRING", "Hello World");

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManagerUpgrade", 1, schema, new DatabaseManager.DefaultListener());

        /* Put data. */
        long id;
        try {
            id = databaseManager.put(oldVersionValue, "COL_INTEGER");
            ContentValues actual = get(databaseManager, id);
            assertNotNull(actual);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }

        /* Upgrade schema. */
        schema.put("COL_INT", 1);

        /* Get instance to access database with a newer schema without handling upgrade. */
        databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "databaseManagerUpgrade", 2, schema, new DatabaseManager.Listener() {

            @Override
            public void onCreate(SQLiteDatabase db) {
            }

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("ALTER TABLE databaseManagerUpgrade ADD COLUMN COL_INT INTEGER");
                return true;
            }
        });
        try {

            /* Verify data still there. */
            ContentValues actual = get(databaseManager, id);
            assertNotNull(actual);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseManager.getRowCount());

            /* Put new data. */
            ContentValues data = new ContentValues();
            data.put("COL_STRING", "Hello World");
            data.put("COL_INT", 2);
            id = databaseManager.put(data, "COL_INTEGER");
            actual = get(databaseManager, id);
            assertNotNull(actual);
            actual.remove("oid");
            assertEquals(data, actual);
            assertEquals(2, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }
    }

    @Test
    public void setMaximumSize() {

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, "test.setMaximumSize", 1, mSchema, new DatabaseManager.DefaultListener());

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

    @Test
    public void replace() {

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        String table = "someTable";
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, table, 1, mSchema, listener);
        String documentIdProperty = "COL_STRING";

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            assertEquals(0L, databaseManager.getRowCount());
            ContentValues contentValues = generateContentValues();
            contentValues.put(documentIdProperty, "some id");
            databaseManager.replace(table, contentValues, documentIdProperty);
            assertEquals(1L, databaseManager.getRowCount());
            databaseManager.replace(table, contentValues, documentIdProperty);
            assertEquals(1L, databaseManager.getRowCount());

            /* Set the documentIdProperty to another value, new row should be created. */
            contentValues = generateContentValues();
            contentValues.put(documentIdProperty, "new id");
            databaseManager.replace(table, contentValues, documentIdProperty);
            assertEquals(2L, databaseManager.getRowCount());

            /* Replace a value with the same document id, if no matching condition given, or multiple matches happened replace will continue to insert. */
            contentValues = generateContentValues();
            contentValues.put(documentIdProperty, "new id");
            databaseManager.replace(table, contentValues);
            databaseManager.replace(table, contentValues, documentIdProperty);
            assertEquals(4L, databaseManager.getRowCount());

            /* Replace by matching an unknown property fails. */
            assertEquals(-1, databaseManager.replace(table, contentValues, "COLUMN_NOT_FOUND"));
            assertEquals(4L, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }
        verify(listener).onCreate(any(SQLiteDatabase.class));
    }

    @Test
    public void replaceByMultipleColumns() {

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        String table = "someTable";
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, table, 1, mSchema, listener);
        String[] documentIdProperties = new String[]{"COL_STRING", "COL_INTEGER"};

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            assertEquals(0L, databaseManager.getRowCount());
            ContentValues contentValues = generateContentValues();
            contentValues.put(documentIdProperties[0], "some id");
            contentValues.put(documentIdProperties[1], 0);
            databaseManager.replace(table, contentValues, documentIdProperties);
            assertEquals(1L, databaseManager.getRowCount());
            databaseManager.replace(table, contentValues, documentIdProperties);
            assertEquals(1L, databaseManager.getRowCount());

            /* Set the documentIdProperty to another value, new row should be created. */
            contentValues = generateContentValues();
            contentValues.put(documentIdProperties[0], "new id");
            contentValues.put(documentIdProperties[1], 1);
            databaseManager.replace(table, contentValues, documentIdProperties);
            assertEquals(2L, databaseManager.getRowCount());

            /* Replace a value with the same document id, if no matching condition given, or multiple matches happened replace will continue to insert. */
            contentValues = generateContentValues();
            contentValues.put(documentIdProperties[0], "new id");
            contentValues.put(documentIdProperties[1], 1);
            databaseManager.replace(table, contentValues);
            databaseManager.replace(table, contentValues, documentIdProperties);
            assertEquals(4L, databaseManager.getRowCount());

            /* Replace by matching an unknown property fails. */
            assertEquals(-1, databaseManager.replace(table, contentValues, "COLUMN_NOT_FOUND", "Random_Column"));
            assertEquals(4L, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }
        verify(listener).onCreate(any(SQLiteDatabase.class));
    }

    @Test
    public void replaceInTableWithUniquenessConstraint() {
        String tableName = "myTable";
        ContentValues content1 = new ContentValues();
        ContentValues content2 = new ContentValues();
        String colStr = "colStr";
        content1.put(colStr, "first string");
        content2.put(colStr, "different string");
        String colInt = "colInt";
        content1.put(colInt, -1);
        content2.put(colInt, -1);
        String colBoolean = "colBoolean";
        content1.put(colBoolean, true);
        content2.put(colBoolean, true);
        DatabaseManager databaseManager = setupTableWithUniqueConstraint(tableName, content1);

        /* Should replace first row because they have the same values in the unique columns. */
        long result = databaseManager.replace(tableName, content2);
        assertTrue(result > 0);
        assertEquals(1L, databaseManager.getRowCount());
        Cursor cursor = databaseManager.getCursor(null, null, null, null);
        ContentValues row = databaseManager.nextValues(cursor);
        assertNotNull(row);
        assertEquals("different string", row.getAsString(colStr));
        assertEquals(-1, row.getAsInteger(colInt).intValue());
        assertEquals(true, row.getAsBoolean(colBoolean));
    }

    private DatabaseManager setupTableWithUniqueConstraint(String tableName, ContentValues contentValues) {

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        DatabaseManager databaseManager = new DatabaseManager(sContext, DATABASE_NAME, tableName, 1, contentValues, listener, new String[]{"colInt", "colBoolean"});
        assertTrue(checkTableExists(databaseManager, tableName));

        /* Insert a new row. */
        long result = databaseManager.getDatabase().insert(tableName, null, contentValues);
        assertTrue(result > 0);
        assertEquals(1L, databaseManager.getRowCount());
        Cursor cursor = databaseManager.getCursor(null, null, null, null);
        ContentValues row = databaseManager.nextValues(cursor);
        assertNotNull(row);
        assertEquals("first string", row.getAsString("colStr"));
        assertEquals(-1, row.getAsInteger("colInt").intValue());
        assertEquals(true, row.getAsBoolean("colBoolean"));
        return databaseManager;
    }

    @Test
    public void insertInTableWithUniquenessConstraint() {
        String tableName = "myTable";
        ContentValues content1 = new ContentValues();
        ContentValues content2 = new ContentValues();
        String colStr = "colStr";
        content1.put(colStr, "first string");
        content2.put(colStr, "different string");
        String colInt = "colInt";
        content1.put(colInt, -1);
        content2.put(colInt, -1);
        String colBoolean = "colBoolean";
        content1.put(colBoolean, true);
        content2.put(colBoolean, true);

        /* Get instance to access database. */
        DatabaseManager databaseManager = setupTableWithUniqueConstraint(tableName, content1);

        /* Insert another row with same unique columns constraint should throw an error. */
        boolean insertThrowsSQLiteConstraintException = false;
        try {
            databaseManager.getDatabase().insertOrThrow(tableName, null, content2);
        } catch (SQLiteConstraintException ex) {
            insertThrowsSQLiteConstraintException = true;
        }
        assertTrue(insertThrowsSQLiteConstraintException);
        assertEquals(1L, databaseManager.getRowCount());
        Cursor cursor = databaseManager.getCursor(null, null, null, null);
        ContentValues row = databaseManager.nextValues(cursor);
        assertNotNull(row);
        assertEquals("first string", row.getAsString(colStr));
        assertEquals(-1, row.getAsInteger(colInt).intValue());
        assertEquals(true, row.getAsBoolean(colBoolean));
    }

    @Test
    public void testMultipleTablesReadWriteDelete() {
        String firstTable = "firstTable";
        ContentValues schema1 = new ContentValues();
        String colStr = "colStr";
        schema1.put(colStr, "str");
        String colInt = "colInt";
        schema1.put(colInt, -1);
        String secondTable = "secondTable";
        ContentValues schema2 = new ContentValues();
        String colBool = "colBool";
        schema2.put(colBool, true);
        String colReal = "colReal";
        schema2.put(colReal, 1.1);

        /* Get instance to access database. */
        DatabaseManager.Listener listener = mock(DatabaseManager.Listener.class);
        DatabaseManager databaseManager =
                new DatabaseManager(
                        sContext,
                        DATABASE_NAME,
                        firstTable,
                        1,
                        schema1,
                        listener);
        databaseManager.createTable(secondTable, schema2, null);
        assertTrue(checkTableExists(databaseManager, firstTable));
        assertTrue(checkTableExists(databaseManager, secondTable));

        schema1 = new ContentValues();
        schema1.put(colStr, "First Table String");
        schema1.put(colInt, 55);
        databaseManager.replace(firstTable, schema1);
        schema2 = new ContentValues();
        schema2.put(colBool, false);
        schema2.put(colReal, 15.41);
        databaseManager.replace(secondTable, schema2);

        Cursor cursor1 = databaseManager.getCursor(firstTable, null, null, null, null);
        cursor1.moveToNext();
        assertEquals("First Table String", cursor1.getString(cursor1.getColumnIndex(colStr)));
        assertEquals(55, cursor1.getInt(cursor1.getColumnIndex(colInt)));
        cursor1.close();

        Cursor cursor2 = databaseManager.getCursor(secondTable, null, null, null, null);
        cursor2.moveToNext();
        int id = cursor2.getInt(cursor2.getColumnIndex(DatabaseManager.PRIMARY_KEY));
        assertTrue(id > 0);
        assertEquals(0, cursor2.getInt(cursor2.getColumnIndex(colBool)));
        assertEquals(15.41, cursor2.getFloat(cursor2.getColumnIndex(colReal)), 0.1);
        cursor2.close();

        /* Delete entry from table 2. */
        int deletedRows = databaseManager.delete(secondTable, DatabaseManager.PRIMARY_KEY, id);
        assertEquals(1, deletedRows);
        cursor2 = databaseManager.getCursor(secondTable, null, null, null, null);
        assertEquals(0, cursor2.getCount());
        cursor2.close();
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
