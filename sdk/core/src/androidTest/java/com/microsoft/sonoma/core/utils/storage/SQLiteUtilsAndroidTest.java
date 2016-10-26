package com.microsoft.sonoma.core.utils.storage;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class SQLiteUtilsAndroidTest {

    @Test
    public void test() {
        new SQLiteUtils();
        assertNotNull(SQLiteUtils.newSQLiteQueryBuilder());
    }
}
