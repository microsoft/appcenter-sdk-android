package com.microsoft.sonoma.crashes.model;

import com.microsoft.sonoma.crashes.model.TestCrashException;

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
