package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("CanBeFinal")
@RunWith(PowerMockRunner.class)
public class AppStoreDetectionTest {

    @Mock
    private Context mContext;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @After
    public void tearDown() {

        /* Reset cache. */
        Whitebox.setInternalState(InstallerUtils.class, "sInstalledFromAppStore", (Boolean) null);
    }

    @Test
    public void init() {
        assertNotNull(new InstallerUtils());
    }

    @Test
    public void nullInstallerIsNotStore() {
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Check cache. */
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    @Test
    public void appStore() {
        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn("com.android.vending");
        assertTrue(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Check cache. */
        assertTrue(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    @Test
    public void adbIsNotStore() {
        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn("adb");
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Check cache. */
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    @Test
    public void localInstallerIsNotStore() {
        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn("com.google.android.packageinstaller");
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Check cache. */
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    @Test
    public void anotherLocalInstallerIsNotStore() {
        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn("com.android.packageinstaller");
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Check cache. */
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }
}
