package avalanche.base;

import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.util.UUID;

import avalanche.base.utils.PrefStorageConstants;
import avalanche.base.utils.StorageHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class AvalancheAndroidTest {

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Application application = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
        Avalanche.useFeatures(application, UUID.randomUUID().toString(), new AvalancheFeature[]{});
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId = Avalanche.getInstallId();
        assertNotNull(installId);
        assertEquals(installId, Avalanche.getInstallId());
        StorageHelper.PreferencesStorage.remove(PrefStorageConstants.KEY_INSTALL_ID);
        UUID installId2 = Avalanche.getInstallId();
        assertNotNull(installId2);
        assertNotEquals(installId2, installId);
    }
}
