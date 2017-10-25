package com.microsoft.appcenter.utils;

import android.util.Log;

import com.microsoft.appcenter.MobileCenter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.verification.VerificationMode;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class MobileCenterLogTest {

    private static void callLogs() {
        MobileCenterLog.logAssert("my-tag", "error with my-tag");
        MobileCenterLog.logAssert("my-tag", "error with my-tag with exception", new Exception());
        MobileCenterLog.error("my-tag", "error with my-tag");
        MobileCenterLog.error("my-tag", "error with my-tag with exception", new Exception());
        MobileCenterLog.warn("my-tag", "warn with my-tag");
        MobileCenterLog.warn("my-tag", "warn with my-tag with exception", new Exception());
        MobileCenterLog.info("my-tag", "info with my-tag");
        MobileCenterLog.info("my-tag", "info with my-tag with exception", new Exception());
        MobileCenterLog.debug("my-tag", "debug with my-tag");
        MobileCenterLog.debug("my-tag", "debug with my-tag with exception", new Exception());
        MobileCenterLog.verbose("my-tag", "verbose with my-tag");
        MobileCenterLog.verbose("my-tag", "verbose with my-tag with exception", new Exception());
    }

    private static void verifyAssert(VerificationMode verificationMode) {
        verifyStatic(verificationMode);
        Log.println(Log.ASSERT, "my-tag", "error with my-tag");
        verifyStatic(verificationMode);
        //noinspection WrongConstant
        Log.println(eq(Log.ASSERT), eq("my-tag"), eq("error with my-tag with exception\nmock stack trace"));
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
        assertEquals(Log.ASSERT, MobileCenter.getLogLevel());
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
    }

    @Before
    public void setUp() {
        mockStatic(Log.class);
        when(Log.getStackTraceString(any(Throwable.class))).thenReturn("mock stack trace");
    }

    @Test
    public void none() {
        MobileCenter.setLogLevel(MobileCenterLog.NONE);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(never());
        verifyAssert(never());
    }

    @Test
    public void assertLevel() {
        MobileCenter.setLogLevel(Log.ASSERT);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(never());
        verifyAssert(times(1));
    }

    @Test
    public void error() {
        MobileCenter.setLogLevel(Log.ERROR);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(never());
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void warn() {
        MobileCenter.setLogLevel(Log.WARN);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(never());
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void info() {
        MobileCenter.setLogLevel(Log.INFO);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(never());
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void debug() {
        MobileCenter.setLogLevel(Log.DEBUG);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(never());
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }

    @Test
    public void verbose() {
        MobileCenter.setLogLevel(Log.VERBOSE);
        assertEquals(MobileCenter.getLogLevel(), MobileCenterLog.getLogLevel());
        callLogs();
        verifyVerbose(times(1));
        verifyDebug(times(1));
        verifyInfo(times(1));
        verifyWarn(times(1));
        verifyError(times(1));
        verifyAssert(times(1));
    }
}
