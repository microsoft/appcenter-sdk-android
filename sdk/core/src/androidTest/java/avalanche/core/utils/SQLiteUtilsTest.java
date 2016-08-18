package avalanche.core.utils;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SQLiteUtilsTest {

    @Test
    public void test() {
        new SQLiteUtils();
        assertNotNull(SQLiteUtils.newSQLiteQueryBuilder());
    }
}
