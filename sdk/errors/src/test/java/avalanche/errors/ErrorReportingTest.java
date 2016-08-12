package avalanche.errors;

import android.os.SystemClock;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.PrefStorageConstants;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.TestCrashException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, StorageHelper.PreferencesStorage.class})
public class ErrorReportingTest {

    @Before
    public void setUp() {
        ErrorReporting.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.PreferencesStorage.class);

        when(SystemClock.elapsedRealtime()).thenReturn(System.currentTimeMillis());

        final String key = PrefStorageConstants.KEY_ENABLED + "_" + ErrorReporting.getInstance().getGroupName();
        PowerMockito.when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                Mockito.when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(key), anyBoolean());
    }

    @Test
    public void singleton() {
        Assert.assertSame(ErrorReporting.getInstance(), ErrorReporting.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = ErrorReporting.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(JavaErrorLog.TYPE) instanceof JavaErrorLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void setEnabled() {
        ErrorReporting.setEnabled(true);
        assertTrue(ErrorReporting.isEnabled());
        assertTrue(ErrorReporting.getInstance().getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof  UncaughtExceptionHandler);
        ErrorReporting.setEnabled(false);
        assertFalse(ErrorReporting.isEnabled());
        assertEquals(ErrorReporting.getInstance().getInitializeTimestamp(), -1);
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        ErrorReporting.setEnabled(true);
        assertTrue(ErrorReporting.isEnabled());
        assertTrue(ErrorReporting.getInstance().getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
    }

    @Test(expected = TestCrashException.class)
    public void generateTestCrash() {
        ErrorReporting.generateTestCrash();
    }

}
