package com.microsoft.sonoma.core;

import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.sonoma.core.utils.PrefStorageConstants;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.core.utils.UUIDUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class SonomaAndroidTest {

    private Application mApplication;

    @Before
    public void setUp() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Sonoma.unsetInstance();
        Constants.APPLICATION_DEBUGGABLE = false;
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Sonoma.start(mApplication, UUIDUtils.randomUUID().toString(), new SonomaFeature[]{});
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId = Sonoma.getInstallId();
        assertNotNull(installId);
        assertEquals(installId, Sonoma.getInstallId());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId2 = Sonoma.getInstallId();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
    }

    @Test
    public void setDefaultLogLevelDebug() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        SonomaLog.setLogLevel(Log.ASSERT);
        Sonoma.start(mApplication, UUIDUtils.randomUUID().toString(), new SonomaFeature[]{});
        assertEquals(Log.WARN, Sonoma.getLogLevel());
    }
}
