package avalanche.core.utils;

import android.util.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import avalanche.core.Avalanche;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class AvalancheLogTest {

    private static void callLogs() {
        AvalancheLog.error("error with default tag");
        AvalancheLog.error("error with default tag with exception", new Exception());
        AvalancheLog.error("my-tag", "error with my-tag");
        AvalancheLog.error("my-tag", "error with my-tag with exception", new Exception());
        AvalancheLog.warn("warn with default tag");
        AvalancheLog.warn("warn with default tag with exception", new Exception());
        AvalancheLog.warn("my-tag", "warn with my-tag");
        AvalancheLog.warn("my-tag", "warn with my-tag with exception", new Exception());
        AvalancheLog.info("info with default tag");
        AvalancheLog.info("info with default tag with exception", new Exception());
        AvalancheLog.info("my-tag", "info with my-tag");
        AvalancheLog.info("my-tag", "info with my-tag with exception", new Exception());
        AvalancheLog.debug("debug with default tag");
        AvalancheLog.debug("debug with default tag with exception", new Exception());
        AvalancheLog.debug("my-tag", "debug with my-tag");
        AvalancheLog.debug("my-tag", "debug with my-tag with exception", new Exception());
        AvalancheLog.verbose("verbose with default tag");
        AvalancheLog.verbose("verbose with default tag with exception", new Exception());
        AvalancheLog.verbose("my-tag", "verbose with my-tag");
        AvalancheLog.verbose("my-tag", "verbose with my-tag with exception", new Exception());
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
        assertEquals(Log.ASSERT, Avalanche.getLogLevel());
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void assertLevel() {
        Avalanche.setLogLevel(Log.ASSERT);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
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
        Avalanche.setLogLevel(Log.ERROR);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
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
        Avalanche.setLogLevel(Log.WARN);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
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
        Avalanche.setLogLevel(Log.INFO);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
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
        Avalanche.setLogLevel(Log.DEBUG);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
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
        Avalanche.setLogLevel(Log.VERBOSE);
        assertEquals(Avalanche.getLogLevel(), AvalancheLog.getLogLevel());
        callLogs();
        PowerMockito.verifyStatic();
        verifyVerbose();
        verifyDebug();
        verifyInfo();
        verifyWarn();
        verifyError();
    }
}
