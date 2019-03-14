/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Intent;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@SuppressWarnings("CanBeFinal")
@PrepareForTest(Distribute.class)
public class DeepLinkActivityTest {

    @Mock
    private Distribute mDistribute;

    @Before
    public void setUp() {
        mockStatic(Distribute.class);
        when(Distribute.getInstance()).thenReturn(mDistribute);
    }

    /**
     * Common code to test invalid intent code path that will also test work around for restart.
     */
    private void invalidIntent(Intent intent) {

        /* Test old browser restart workaround. */
        when(intent.cloneFilter()).thenReturn(intent);
        when(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).thenReturn(intent);
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        activity.onCreate(null);

        /* Check interactions. */
        verify(activity).startActivity(intent);
        verify(activity).finish();
        verifyStatic(never());
        Distribute.getInstance();
    }

    @Test
    public void missingParametersAndRestartWorkaround() {
        Intent intent = mock(Intent.class);
        invalidIntent(intent);
    }

    @Test
    public void missingRequestId() {
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_UPDATE_TOKEN)).thenReturn("mock");
        invalidIntent(intent);
    }

    @Test
    public void missingDistributionGroupId() {
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock");
        invalidIntent(intent);
    }

    @Test
    public void validWithUpdateSetupFailedAndNoTaskRoot() {

         /* Build valid intent. */
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock1");
        when(intent.getStringExtra(DistributeConstants.EXTRA_UPDATE_SETUP_FAILED)).thenReturn("mock2");
        when(intent.getFlags()).thenReturn(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        /* Start activity. */
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        activity.onCreate(null);

         /* Verify interactions. */
        verify(activity, never()).startActivity(any(Intent.class));
        verify(mDistribute, never()).storeRedirectionParameters(anyString(), anyString(), anyString());
        verify(activity).finish();
        verify(mDistribute).storeUpdateSetupFailedParameter("mock1", "mock2");
    }

    @Test
    public void validWithTesterAppUpdateSetupFailedAndNoTaskRoot() {

         /* Build valid intent. */
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock1");
        when(intent.getStringExtra(DistributeConstants.EXTRA_TESTER_APP_UPDATE_SETUP_FAILED)).thenReturn("mock2");
        when(intent.getFlags()).thenReturn(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        /* Start activity. */
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        activity.onCreate(null);

         /* Verify interactions. */
        verify(activity, never()).startActivity(any(Intent.class));
        verify(mDistribute, never()).storeRedirectionParameters(anyString(), anyString(), anyString());
        verify(activity).finish();
        verify(mDistribute).storeTesterAppUpdateSetupFailedParameter("mock1", "mock2");
    }

    @Test
    public void validAndNoTaskRoot() {

        /* Build valid intent. */
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock1");
        when(intent.getStringExtra(DistributeConstants.EXTRA_DISTRIBUTION_GROUP_ID)).thenReturn("mock2");
        when(intent.getStringExtra(DistributeConstants.EXTRA_UPDATE_TOKEN)).thenReturn("mock3");
        when(intent.getFlags()).thenReturn(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        /* Start activity. */
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        activity.onCreate(null);

        /* Verify interactions. */
        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity).finish();
        verify(mDistribute).storeRedirectionParameters("mock1", "mock2", "mock3");
    }

    @Test
    public void validAndTaskRootNoLauncher() {

        /* Build valid intent. */
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock1");
        when(intent.getStringExtra(DistributeConstants.EXTRA_DISTRIBUTION_GROUP_ID)).thenReturn("mock2");
        when(intent.getFlags()).thenReturn(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        /* Start activity. */
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        when(activity.isTaskRoot()).thenReturn(true);
        when(activity.getPackageManager()).thenReturn(mock(PackageManager.class));
        activity.onCreate(null);

        /* Verify interactions. */
        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity).finish();
        verify(mDistribute).storeRedirectionParameters("mock1", "mock2", null);
    }

    @Test
    public void validAndTaskRootStartLauncher() {

        /* Build valid intent. */
        Intent intent = mock(Intent.class);
        when(intent.getStringExtra(DistributeConstants.EXTRA_REQUEST_ID)).thenReturn("mock1");
        when(intent.getStringExtra(DistributeConstants.EXTRA_DISTRIBUTION_GROUP_ID)).thenReturn("mock2");
        when(intent.getFlags()).thenReturn(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        /* Start activity. */
        DeepLinkActivity activity = spy(new DeepLinkActivity());
        when(activity.getIntent()).thenReturn(intent);
        when(activity.isTaskRoot()).thenReturn(true);
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageName()).thenReturn("mock.package");
        Intent launcherIntent = mock(Intent.class);
        when(packageManager.getLaunchIntentForPackage("mock.package")).thenReturn(launcherIntent);
        when(activity.getPackageManager()).thenReturn(packageManager);
        activity.onCreate(null);

        /* Verify interactions. */
        verify(activity).startActivity(launcherIntent);
        verify(activity).finish();
        verify(mDistribute).storeRedirectionParameters("mock1", "mock2", null);
    }
}
