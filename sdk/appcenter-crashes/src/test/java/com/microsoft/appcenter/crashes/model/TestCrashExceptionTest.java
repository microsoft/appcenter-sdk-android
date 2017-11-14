package com.microsoft.appcenter.crashes.model;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class TestCrashExceptionTest {

    @Test
    public void newInstance() {
        TestCrashException e = new TestCrashException();
        Assert.assertEquals(TestCrashException.CRASH_MESSAGE, e.getMessage());
    }
}
