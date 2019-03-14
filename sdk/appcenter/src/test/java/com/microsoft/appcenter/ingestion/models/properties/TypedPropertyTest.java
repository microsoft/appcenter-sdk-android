/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import com.microsoft.appcenter.ingestion.models.CommonProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class TypedPropertyTest {

    @Test
    public void equalsHashCode() {
        TypedProperty a = new MockTypedProperty();
        TypedProperty b = new MockTypedProperty();
        checkNotEquals(a, null);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
    }

    @Test(expected = JSONException.class)
    public void readDifferentTypeTest() throws JSONException {
        JSONObject mockJsonObject = mock(JSONObject.class);
        when(mockJsonObject.getString(CommonProperties.TYPE)).thenReturn("type");
        TypedProperty mockLog = new MockTypedProperty();
        mockLog.read(mockJsonObject);
    }

    private static class MockTypedProperty extends TypedProperty {

        @Override
        public String getType() {
            return null;
        }
    }
}
