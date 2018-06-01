package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class DataTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new Data());
    }

    @Test
    public void equalsHashCode() throws JSONException {

        /* Empty objects. */
        Data a = new Data();
        Data b = new Data();
        checkEquals(a, b);

        /* Properties. */
        a.getProperties().put("a", "b");
        checkNotEquals(a, b);
        b.getProperties().put("a", "b");
        checkEquals(a, b);
    }
}
