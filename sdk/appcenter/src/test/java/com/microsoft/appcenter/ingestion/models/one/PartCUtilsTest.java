package com.microsoft.appcenter.ingestion.models.one;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Data.class)
public class PartCUtilsTest {

    @Test
    public void coverJSONException() throws Exception {

        /* Fake JSON exception to cover the checked exception that never happens. */
        JSONObject value = mock(JSONObject.class);
        whenNew(JSONObject.class).withNoArguments().thenReturn(value);
        when(value.put(anyString(), any())).thenThrow(new JSONException("mock"));
        CommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        PartCUtils.addPartCFromLog(properties, commonSchemaLog);
        assertEquals(0, commonSchemaLog.getData().getProperties().length());
    }
}
