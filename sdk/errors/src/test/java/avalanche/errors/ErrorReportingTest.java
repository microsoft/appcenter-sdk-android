package avalanche.errors;

import org.junit.Test;

import avalanche.errors.model.TestCrashException;

public class ErrorReportingTest {
    
    @Test(expected = TestCrashException.class)
    public void generateTestCrash() {
        ErrorReporting.generateTestCrash();
    }
}
