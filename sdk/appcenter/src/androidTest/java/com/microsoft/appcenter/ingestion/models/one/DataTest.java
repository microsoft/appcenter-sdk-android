/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Test;
import org.mockito.InOrder;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void serializeOrder() throws JSONException {

        /* When properties not inserted in order. */
        Data d = new Data();
        d.getProperties().put("a", "b");
        d.getProperties().put("baseData", new JSONObject());
        d.getProperties().put("baseType", "type");

        /* When serializing properties. */
        JSONStringer writer = mock(JSONStringer.class);
        when(writer.key(anyString())).thenReturn(writer);
        d.write(writer);

        /* Then baseType is written, then baseData, then Part C. */
        InOrder inOrder = inOrder(writer);
        inOrder.verify(writer).key("baseType");
        inOrder.verify(writer).key("baseData");
        inOrder.verify(writer).key("a");
    }
}
