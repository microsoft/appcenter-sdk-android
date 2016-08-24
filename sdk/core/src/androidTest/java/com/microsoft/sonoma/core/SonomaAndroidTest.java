package com.microsoft.sonoma.core;

import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;

import com.microsoft.sonoma.core.utils.PrefStorageConstants;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.core.utils.UUIDUtils;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class SonomaAndroidTest {

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Application application = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
        Sonoma.start(application, UUIDUtils.randomUUID().toString(), new SonomaFeature[]{});
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId = Sonoma.getInstallId();
        assertNotNull(installId);
        assertEquals(installId, Sonoma.getInstallId());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId2 = Sonoma.getInstallId();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
    }
}
