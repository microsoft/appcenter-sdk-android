/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.ComponentName;
import android.content.Intent;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributeTest extends AbstractDistributeTest {

    @Test
    public void singleton() {
        assertSame(Distribute.getInstance(), Distribute.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Distribute.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(DistributionStartSessionLog.TYPE) instanceof DistributionStartSessionLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitialization() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launch one. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onActivityCreated(mActivity, null);

        /* No exceptions. */
    }
}
