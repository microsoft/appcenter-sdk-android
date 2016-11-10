package com.microsoft.azure.mobile;

import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class MobileCenterAndroidTest {

    private Application mApplication;

    @Before
    public void setUp() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        MobileCenter.unsetInstance();
        Constants.APPLICATION_DEBUGGABLE = false;
        mApplication = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        MobileCenter.start(mApplication, UUIDUtils.randomUUID().toString());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId = MobileCenter.getInstallId();
        assertNotNull(installId);
        assertEquals(installId, MobileCenter.getInstallId());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId2 = MobileCenter.getInstallId();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
    }

    @Test
    public void setDefaultLogLevelDebug() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        MobileCenterLog.setLogLevel(Log.ASSERT);
        MobileCenter.start(mApplication, UUIDUtils.randomUUID().toString());
        assertEquals(Log.WARN, MobileCenter.getLogLevel());
    }
}
