/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Data.class)
public class CommonSchemaDataUtilsTest {

    @Test
    public void coverJSONException() throws Exception {

        /* Fake JSON exception to cover the checked exception that never happens. */
        JSONObject value = mock(JSONObject.class);
        whenNew(JSONObject.class).withNoArguments().thenReturn(value);
        when(value.put(anyString(), any())).thenThrow(new JSONException("mock"));
        CommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName("a");
        stringTypedProperty.setValue("b");
        properties.add(stringTypedProperty);
        CommonSchemaDataUtils.addCommonSchemaData(properties, commonSchemaLog);
        assertEquals(0, commonSchemaLog.getData().getProperties().length());
    }
}
