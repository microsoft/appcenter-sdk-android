package com.microsoft.appcenter.ingestion.models.json;

import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONStringer;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Supplements tests in LogSerializerTest.
 */
@SuppressWarnings("unused")
@PrepareForTest({DefaultLogSerializer.class, AppCenterLog.class})
public class DefaultLogSerializerTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void failToUsePrettyJson() throws Exception {

        /* Mock logs to verify interactions. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);

        /* Mock stub JSONStringer. */
        JSONStringer stringer = mock(JSONStringer.class);
        whenNew(JSONStringer.class).withAnyArguments().thenReturn(stringer);
        when(stringer.key(anyString())).thenReturn(stringer);
        when(stringer.toString()).thenReturn("{}");

        /*
         * We are in test folder so the stub does not contain the private methods,
         * this test thus simulates what happens if we can't access the private methods
         * via reflection to use pretty json. We check it serializes gracefully.
         */
        assertEquals("{}", new DefaultLogSerializer().serializeContainer(mock(LogContainer.class)));

        /* And that it logs why the pretty json could not be used. */
        verifyStatic();
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString(), any(NoSuchMethodError.class));
    }
}
