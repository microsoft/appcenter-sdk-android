package com.microsoft.sonoma.core.utils;

import android.util.Log;

import com.microsoft.sonoma.core.Sonoma;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class SonomaLogTest {

    private static void callLogs() {
        SonomaLog.error("error with default tag");
        SonomaLog.error("error with default tag with exception", new Exception());
        SonomaLog.error("my-tag", "error with my-tag");
        SonomaLog.error("my-tag", "error with my-tag with exception", new Exception());
        SonomaLog.warn("warn with default tag");
        SonomaLog.warn("warn with default tag with exception", new Exception());
        SonomaLog.warn("my-tag", "warn with my-tag");
        SonomaLog.warn("my-tag", "warn with my-tag with exception", new Exception());
        SonomaLog.info("info with default tag");
        SonomaLog.info("info with default tag with exception", new Exception());
        SonomaLog.info("my-tag", "info with my-tag");
        SonomaLog.info("my-tag", "info with my-tag with exception", new Exception());
        SonomaLog.debug("debug with default tag");
        SonomaLog.debug("debug with default tag with exception", new Exception());
        SonomaLog.debug("my-tag", "debug with my-tag");
        SonomaLog.debug("my-tag", "debug with my-tag with exception", new Exception());
        SonomaLog.verbose("verbose with default tag");
        SonomaLog.verbose("verbose with default tag with exception", new Exception());
        SonomaLog.verbose("my-tag", "verbose with my-tag");
        SonomaLog.verbose("my-tag", "verbose with my-tag with exception", new Exception());
    }

    private static void verifyError() {
        Log.e(anyString(), eq("error with default tag"));
        Log.e(anyString(), eq("error with default tag with exception"), any(Exception.class));
        Log.e("my-tag", "error with my-tag");
        Log.e(eq("my-tag"), eq("error with my-tag with exception"), any(Exception.class));
    }

    private static void verifyWarn() {
        Log.w(anyString(), eq("warn with default tag"));
        Log.w(anyString(), eq("warn with default tag with exception"), any(Exception.class));
        Log.w("my-tag", "warn with my-tag");
        Log.w(eq("my-tag"), eq("warn with my-tag with exception"), any(Exception.class));
    }

    private static void verifyInfo() {
        Log.i(anyString(), eq("info with default tag"));
        Log.i(anyString(), eq("info with default tag with exception"), any(Exception.class));
        Log.i("my-tag", "info with my-tag");
        Log.i(eq("my-tag"), eq("info with my-tag with exception"), any(Exception.class));
    }

    private static void verifyDebug() {
        Log.d(anyString(), eq("debug with default tag"));
        Log.d(anyString(), eq("debug with default tag with exception"), any(Exception.class));
        Log.d("my-tag", "debug with my-tag");
        Log.d(eq("my-tag"), eq("debug with my-tag with exception"), any(Exception.class));
    }

    private static void verifyVerbose() {
        Log.v(anyString(), eq("verbose with default tag"));
        Log.v(anyString(), eq("verbose with default tag with exception"), any(Exception.class));
        Log.v("my-tag", "verbose with my-tag");
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
        PowerMockito.verifyStatic(never());
        verifyVerbose();
        verifyDebug();
        verifyInfo();
        verifyWarn();
        verifyError();
    }

    @Test
    public void error() {
        Sonoma.setLogLevel(Log.ERROR);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic(never());
        verifyVerbose();
        verifyDebug();
        verifyInfo();
        verifyWarn();
        PowerMockito.verifyStatic();
        verifyError();
    }

    @Test
    public void warn() {
        Sonoma.setLogLevel(Log.WARN);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic(never());
        verifyVerbose();
        verifyDebug();
        verifyInfo();
        PowerMockito.verifyStatic();
        verifyWarn();
        verifyError();
    }

    @Test
    public void info() {
        Sonoma.setLogLevel(Log.INFO);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic(never());
        verifyVerbose();
        verifyDebug();
        PowerMockito.verifyStatic();
        verifyInfo();
        verifyWarn();
        verifyError();
    }

    @Test
    public void debug() {
        Sonoma.setLogLevel(Log.DEBUG);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic(never());
        verifyVerbose();
        PowerMockito.verifyStatic();
        verifyDebug();
        verifyInfo();
        verifyWarn();
        verifyError();
    }

    @Test
    public void verbose() {
        Sonoma.setLogLevel(Log.VERBOSE);
        assertEquals(Sonoma.getLogLevel(), SonomaLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic();
        verifyVerbose();
        verifyDebug();
        verifyInfo();
        verifyWarn();
        verifyError();
    }
}
