/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
        new InstallerUtils();
    }

    @Test
    public void nullInstallerIsNotStore() {

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void installerUtilsAlreadyInitialized() {
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));

        /* Verify called once. */
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    @Test
    public void appStore() {
        setInstallerPackageName("com.android.vending");

        /* Check cache. */
        verifyFromAppStore();
    }

    @Test
    public void adbIsNotStore() {
        setInstallerPackageName("adb");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void localInstallerIsNotStore() {
        setInstallerPackageName("com.google.android.packageinstaller");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void anotherLocalInstallerIsNotStore() {
        setInstallerPackageName("com.android.packageinstaller");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void managedProvisioningIsNotStore() {
        setInstallerPackageName("com.android.managedprovisioning");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void miUiLocalInstallerIsNotStore() {
        setInstallerPackageName("com.miui.packageinstaller");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    @Test
    public void samsungLocalInstallerIsNotStore() {
        setInstallerPackageName("com.samsung.android.packageinstaller");

        /* Check cache. */
        verifyNotFromAppStore();
    }

    private void setInstallerPackageName(String packageName) {
        when(mPackageManager.getInstallerPackageName(anyString())).thenReturn(packageName);
    }

    private void verifyNotFromAppStore() {
        assertFalse(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }

    private void verifyFromAppStore() {
        assertTrue(InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext));
        verify(mPackageManager).getInstallerPackageName(anyString());
    }
}
