package avalanche.core;

import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.util.UUID;

import avalanche.core.utils.PrefStorageConstants;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class AvalancheAndroidTest {

    @Test
    public void getInstallId() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Application application = Instrumentation.newApplication(Application.class, InstrumentationRegistry.getTargetContext());
        Avalanche.start(application, UUIDUtils.randomUUID().toString(), new AvalancheFeature[]{});
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
