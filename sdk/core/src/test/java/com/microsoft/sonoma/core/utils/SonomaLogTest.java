package com.microsoft.sonoma.core.utils;

import android.util.Log;

import com.microsoft.sonoma.core.Sonoma;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.verification.VerificationMode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class SonomaLogTest {

    private static void callLogs() {
        SonomaLog.error("my-tag", "error with my-tag");
        SonomaLog.error("my-tag", "error with my-tag with exception", new Exception());
        SonomaLog.warn("my-tag", "warn with my-tag");
        SonomaLog.warn("my-tag", "warn with my-tag with exception", new Exception());
        SonomaLog.info("my-tag", "info with my-tag");
        SonomaLog.info("my-tag", "info with my-tag with exception", new Exception());
        SonomaLog.debug("my-tag", "debug with my-tag");
        SonomaLog.debug("my-tag", "debug with my-tag with exception", new Exception());
        SonomaLog.verbose("my-tag", "verbose with my-tag");
        SonomaLog.verbose("my-tag", "verbose with my-tag with exception", new Exception());
    }

    private static void verifyError(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.e("my-tag", "error with my-tag");
        verifyStatic(verificationMode);
        Log.e(eq("my-tag"), eq("error with my-tag with exception"), any(Exception.class));
    }

    private static void verifyWarn(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.w("my-tag", "warn with my-tag");
        verifyStatic(verificationMode);
        Log.w(eq("my-tag"), eq("warn with my-tag with exception"), any(Exception.class));
    }

    private static void verifyInfo(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.i("my-tag", "info with my-tag");
        verifyStatic(verificationMode);
        Log.i(eq("my-tag"), eq("info with my-tag with exception"), any(Exception.class));
    }

    private static void verifyDebug(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.d("my-tag", "debug with my-tag");
        verifyStatic(verificationMode);
        Log.d(eq("my-tag"), eq("debug with my-tag with exception"), any(Exception.class));
    }

    private static void verifyVerbose(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.v("my-tag", "verbose with my-tag");
        verifyStatic(verificationMode);
        Log.v(eq("my-tag"), eq("verbose with my-tag with exception"), any(Exception.class));
    }

    @BeforeClass
    public static void setUpBeforeClass() {

        /* Default initial state can be tested only once in the entire test suite... */
        assertEquals(Log.ASSERT, Sonoma.getLogLevel());
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void assertLevel() {
        Sonoma.setLogLevel(Log.ASSERT);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(never());
    }

    @Test
    public void error() {
        Sonoma.setLogLevel(Log.ERROR);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(times(1));
    }

    @Test
    public void warn() {
        Sonoma.setLogLevel(Log.WARN);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(times(1));
        verifyError(times(1));
    }

    @Test
    public void info() {
        Sonoma.setLogLevel(Log.INFO);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
    }

    @Test
    public void debug() {
        Sonoma.setLogLevel(Log.DEBUG);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
    }

    @Test
    public void verbose() {
        Sonoma.setLogLevel(Log.VERBOSE);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        verifyVerbose(times(1));
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
    }
}
