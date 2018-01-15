package com.microsoft.appcenter.codepush;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CodePushAndroidTest {

    @Test
    public void useAppContext() throws Exception {
        
        /* Context of the app under test. */
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.microsoft.appcenter.codepush.test", appContext.getPackageName());
    }
}
