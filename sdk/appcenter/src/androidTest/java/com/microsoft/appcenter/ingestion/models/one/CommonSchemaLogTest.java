/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.junit.Test;

import java.util.Date;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class CommonSchemaLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new MockCommonSchemaLog());
    }

    @Test
    public void equalsHashCode() throws JSONException {

        /* Empty objects. */
        MockCommonSchemaLog a = new MockCommonSchemaLog();
        MockCommonSchemaLog b = new MockCommonSchemaLog();
        checkEquals(a, b);

        /* Ver. */
        a.setVer("a1");
        checkNotEquals(a, b);
        b.setVer("b1");
        checkNotEquals(a, b);
        b.setVer("a1");
        checkEquals(a, b);

        /* Name. */
        a.setName("a2");
        checkNotEquals(a, b);
        b.setName("b2");
        checkNotEquals(a, b);
        b.setName("a2");
        checkEquals(a, b);

        /* Time. */
        a.setTimestamp(new Date(0));
        checkNotEquals(a, b);
        b.setTimestamp(new Date(1));
        checkNotEquals(a, b);
        b.setTimestamp(new Date(0));
        checkEquals(a, b);

        /* Pop sample. */
        a.setPopSample(1.0);
        checkNotEquals(a, b);
        b.setPopSample(2.2);
        checkNotEquals(a, b);
        b.setPopSample(1.0);
        checkEquals(a, b);

        /* iKey. */
        a.setIKey("a3");
        checkNotEquals(a, b);
        b.setIKey("b3");
        checkNotEquals(a, b);
        b.setIKey("a3");
        checkEquals(a, b);

        /* Flags. */
        a.setFlags(4L);
        checkNotEquals(a, b);
        b.setFlags(5L);
        checkNotEquals(a, b);
        b.setFlags(4L);
        checkEquals(a, b);

        /* CV. */
        a.setCV("a4");
        checkNotEquals(a, b);
        b.setCV("b4");
        checkNotEquals(a, b);
        b.setCV("a4");
        checkEquals(a, b);

        /* Extensions. */
        Extensions ext = new Extensions();
        ext.setLoc(new LocExtension());
        a.setExt(ext);
        checkNotEquals(a, b);
        b.setExt(new Extensions());
        checkNotEquals(a, b);
        b.setExt(a.getExt());
        checkEquals(a, b);

        /* Data. */
        Data data = new Data();
        data.getProperties().put("a", "b");
        a.setData(data);
        checkNotEquals(a, b);
        b.setData(a.getData());
        checkEquals(a, b);
    }
}
