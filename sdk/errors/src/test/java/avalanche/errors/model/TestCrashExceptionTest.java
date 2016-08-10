package avalanche.errors.model;

import junit.framework.Assert;

import org.junit.Test;

public class TestCrashExceptionTest {

    @Test
    public void instantiation1() {
        TestCrashException e = new TestCrashException();
        Assert.assertEquals(TestCrashException.EXCEPTION_MESSAGE, e.getMessage());
    }

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @Test(expected = UnsupportedOperationException.class)
    public void instantiation2() {
        new TestCrashException("");
    }

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @Test(expected = UnsupportedOperationException.class)
    public void instantiation3() {
        new TestCrashException("", new Exception());
    }

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @Test(expected = UnsupportedOperationException.class)
    public void instantiation4() {
        new TestCrashException(new Exception());
    }
}
