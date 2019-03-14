/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.os.SystemClock;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, SharedPreferencesManager.class, AppCenterLog.class, AppCenter.class, HandlerUtils.class})
abstract class AbstractAnalyticsTest {

    static final String ANALYTICS_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Analytics.getInstance().getServiceName();

    @Mock
    AppCenterHandler mAppCenterHandler;

    @Mock
    AppCenter mAppCenter;

    @Mock
    private AppCenterFuture<Boolean> mCoreEnabledFuture;

    @Before
    public void setUp() {
        Analytics.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        when(AppCenter.isConfigured()).thenReturn(true);
        when(AppCenter.getInstance()).thenReturn(mAppCenter);
        when(AppCenter.isEnabled()).thenReturn(mCoreEnabledFuture);
        when(mCoreEnabledFuture.get()).thenReturn(true);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(eq(key), eq(true))).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(anyString(), anyBoolean());

        /* Pretend automatic page tracking is enabled by default, this will be the case if service becomes public. */
        // TODO remove that after that feature becomes public and thus a default.
        assertFalse(Analytics.isAutoPageTrackingEnabled());
        Analytics.setAutoPageTrackingEnabled(true);
    }
}
