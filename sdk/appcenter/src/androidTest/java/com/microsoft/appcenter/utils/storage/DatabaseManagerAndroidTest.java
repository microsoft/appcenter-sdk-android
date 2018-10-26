package com.microsoft.appcenter.utils.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import static com.microsoft.appcenter.utils.storage.DatabaseManager.ALLOWED_SIZE_MULTIPLE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @AfterClass
    public static void tearDownClass() {

        /* Delete database. */
        sContext.deleteDatabase("test-databaseManager");
        sContext.deleteDatabase("test-databaseManagerUpgrade");
        sContext.deleteDatabase("test-databaseManagerScannerRemove");
        sContext.deleteDatabase("test-databaseManagerScannerNext");
        sContext.deleteDatabase("test-setMaximumSize");
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void runDatabaseManagerTest(DatabaseManager databaseManager) {
        ContentValues value1 = generateContentValues();
        ContentValues value2 = generateContentValues();
        ContentValues value3 = generateContentValues();

        /* Put. */
        Long value1Id = databaseManager.put(value1);
        assertNotNull(value1Id);

        /* Put another. */
        Long value2Id = databaseManager.put(value2);
        assertNotNull(value2Id);

        /* Generate an ID that is neither value1Id nor value2Id. */

        /* Get. */
        ContentValues value1FromDatabase = databaseManager.get(value1Id);
        assertContentValuesEquals(value1, value1FromDatabase);
        ContentValues value2FromDatabase = databaseManager.get(DatabaseManager.PRIMARY_KEY, value2Id);
        assertContentValuesEquals(value2, value2FromDatabase);
        //noinspection ResourceType
        ContentValues nullValueFromDatabase = databaseManager.get(-1);
        assertNull(nullValueFromDatabase);

        /* Count with scanner. */
        DatabaseManager.Scanner scanner = databaseManager.getScanner();
        assertEquals(2, scanner.getCount());
        assertEquals(2, scanner.getCount());
        SQLiteQueryBuilder queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING = ?");
        DatabaseManager.Scanner scanner1 = databaseManager.getScanner(queryBuilder, new String[]{value1.getAsString("COL_STRING")}, false);
        assertEquals(1, scanner1.getCount());
        Iterator<ContentValues> iterator = scanner1.iterator();
        assertContentValuesEquals(value1, iterator.next());
        assertFalse(iterator.hasNext());

        /* Null value matching. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING IS NULL");
        assertEquals(0, databaseManager.getScanner(queryBuilder, null, false).getCount());
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING_NULL IS NULL");
        assertEquals(2, databaseManager.getScanner(queryBuilder, null, false).getCount());

        /* Test null value filter does not exclude anything, so returns the 2 logs. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        scanner = databaseManager.getScanner(queryBuilder, null, false);
        assertEquals(2, scanner.getCount());

        /* Test filtering only with the second key parameter to get only the second log. */
        queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        queryBuilder.appendWhere("COL_STRING NOT IN (?)");
        scanner = databaseManager.getScanner(queryBuilder, new String[]{value1.getAsString("COL_STRING")}, false);
        assertEquals(1, scanner.getCount());
        assertContentValuesEquals(value2, scanner.iterator().next());

        /* Delete. */
        databaseManager.delete(value1Id);
        assertNull(databaseManager.get(value1Id));
        assertEquals(1, databaseManager.getRowCount());
        assertEquals(1, databaseManager.getScanner().getCount());

        /* Put logs to delete multiple IDs. */
        ContentValues value4 = generateContentValues();
        ContentValues value5 = generateContentValues();
        Long value4Id = databaseManager.put(value4);
        Long value5Id = databaseManager.put(value5);
        assertNotNull(value4Id);
        assertNotNull(value5Id);

        /* Delete multiple logs. */
        databaseManager.delete(Arrays.asList(value4Id, value5Id));
        assertNull(databaseManager.get(value4Id));
        assertNull(databaseManager.get(value5Id));
        assertEquals(1, databaseManager.getRowCount());

        /* Put logs to delete with condition. */
        ContentValues value6 = generateContentValues();
        ContentValues value7 = generateContentValues();
        value6.put("COL_STRING", value2.getAsString("COL_STRING"));
        value7.put("COL_STRING", value2.getAsString("COL_STRING") + "A");
        Long value6Id = databaseManager.put(value6);
        Long value7Id = databaseManager.put(value7);
        assertNotNull(value6Id);
        assertNotNull(value7Id);

        /* Delete logs with condition. */
        databaseManager.delete("COL_STRING", value2.getAsString("COL_STRING"));
        assertEquals(1, databaseManager.getRowCount());
        ContentValues value7FromDatabase = databaseManager.get(value7Id);
        assertContentValuesEquals(value7, value7FromDatabase);

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
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-databaseManager", "databaseManager", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            runDatabaseManagerTest(databaseManager);
        } finally {

            /* Close. */
            //noinspection ThrowFromFinallyBlock
            databaseManager.close();
        }
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
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-databaseManagerUpgrade", "databaseManagerUpgrade", 1, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });
        try {

            /* Database will always create a column for identifiers so default length of all tables is 1. */
            assertEquals(2, databaseManager.getColumnNames().length);
            long id = databaseManager.put(oldVersionValue);

            /* Put data. */
            ContentValues actual = databaseManager.get(id);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }

        /* Get instance to access database with a newer schema without handling upgrade. */
        databaseManager = new DatabaseManager(sContext, "test-databaseManagerUpgrade", "databaseManagerUpgrade", 2, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        /* Verify data deleted since no handled upgrade. */
        try {
            assertEquals(11, databaseManager.getColumnNames().length);
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
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-databaseManagerUpgrade", "databaseManagerUpgrade", 1, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        /* Put data. */
        long id;
        try {
            id = databaseManager.put(oldVersionValue);
            ContentValues actual = databaseManager.get(id);
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
        databaseManager = new DatabaseManager(sContext, "test-databaseManagerUpgrade", "databaseManagerUpgrade", 2, schema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("ALTER TABLE databaseManagerUpgrade ADD COLUMN COL_INT INTEGER");
                return true;
            }
        });
        try {

            /* Verify data still there. */
            ContentValues actual = databaseManager.get(id);
            actual.remove("oid");
            assertEquals(oldVersionValue, actual);
            assertEquals(1, databaseManager.getRowCount());

            /* Put new data. */
            ContentValues data = new ContentValues();
            data.put("COL_STRING", "Hello World");
            data.put("COL_INT", 2);
            id = databaseManager.put(data);
            actual = databaseManager.get(id);
            actual.remove("oid");
            assertEquals(data, actual);
            assertEquals(2, databaseManager.getRowCount());
        } finally {

            /* Close. */
            databaseManager.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void databaseManagerScannerRemove() {

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-databaseManagerScannerRemove", "databaseManagerScannerRemove", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            databaseManager.getScanner().iterator().remove();
        } finally {

            /* Close. */
            //noinspection ThrowFromFinallyBlock
            databaseManager.close();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void databaseManagerScannerNext() {

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-databaseManagerScannerNext", "databaseManagerScannerNext", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {
            databaseManager.getScanner().iterator().next();
        } finally {

            /* Close. */
            //noinspection ThrowFromFinallyBlock
            databaseManager.close();
        }
    }

    @Test
    public void setMaximumSize() {

        /* Get instance to access database. */
        DatabaseManager databaseManager = new DatabaseManager(sContext, "test-setMaximumSize", "test.setMaximumSize", 1, mSchema, new DatabaseManager.Listener() {

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                return false;
            }
        });

        //noinspection TryFinallyCanBeTryWithResources (try with resources statement is API >= 19)
        try {

            /* Test to change to an exact size as its multiple of 4KB. */
            assertTrue(databaseManager.setMaxSize(MAX_SIZE_IN_BYTES));
            assertEquals(MAX_SIZE_IN_BYTES, databaseManager.getMaxSize());

            /* Test inexact value, it will use next multiple of 4KB. */
            long desiredSize = MAX_SIZE_IN_BYTES * 2 + 1;
            assertTrue(databaseManager.setMaxSize(desiredSize));
            assertEquals(desiredSize - 1 + ALLOWED_SIZE_MULTIPLE, databaseManager.getMaxSize());

            /* Try to set to a very small value. */
            assertFalse(databaseManager.setMaxSize(2));

            /* Test the side effect is that we shrunk to the minimum size that is possible. */
            assertEquals(MAX_SIZE_IN_BYTES, databaseManager.getMaxSize());
        } finally {

            /* Close. */
            //noinspection ThrowFromFinallyBlock
            databaseManager.close();
        }
    }

}