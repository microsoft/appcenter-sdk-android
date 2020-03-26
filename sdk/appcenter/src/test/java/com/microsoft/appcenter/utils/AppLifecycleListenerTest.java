/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;

import com.microsoft.appcenter.AppCenterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PrepareForTest({Handler.class})
public class AppLifecycleListenerTest {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testRegisterApplicationLifecycleCallbacks() {
        Handler mockHandler = mock(Handler.class);
        ApplicationLifecycleListener applicationLifecycleListener = new ApplicationLifecycleListener(mockHandler);
        Application mockApplication = mock(Application.class);
        AppCenterService mockAppCenterService = mock(AppCenterService.class);

        // Verify that registerActivityLifecycleCallbacks was called.
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService);
        verify(mockApplication).registerActivityLifecycleCallbacks(any(ApplicationLifecycleListener.class));

        // Verify that registerActivityLifecycleCallbacks wasn't called.
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService);
        verify(mockApplication, times(0)).registerActivityLifecycleCallbacks(any(ApplicationLifecycleListener.class));
    }

    @Test
    public void onActivityDoubleResumedTest() {

        // Prepare data.
        Handler mockHandler = mock(Handler.class);
        ApplicationLifecycleListener applicationLifecycleListener = new ApplicationLifecycleListener(mockHandler);
        Activity mockActivity = mock(Activity.class);
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        applicationLifecycleListener.onActivityResumed(mockActivity);
        verify(mockHandler, times(0)).removeCallbacks(any(Runnable.class));

        // Call ActivityResume again.
        applicationLifecycleListener.onActivityResumed(mockActivity);
        verify(mockHandler, times(0)).removeCallbacks(any(Runnable.class));

        // Pause.
        applicationLifecycleListener.onActivityPaused(mockActivity);
        verify(mockHandler, times(0)).postDelayed(any(Runnable.class), anyLong());

        applicationLifecycleListener.onActivityPaused(mockActivity);
        verify(mockHandler).postDelayed(any(Runnable.class), anyLong());

        // Start.
        applicationLifecycleListener.onActivityResumed(mockActivity);
        verify(mockHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void onActivityDoubleStartTest() {

        // Prepare data.
        Handler mockHandler = mock(Handler.class);
        ApplicationLifecycleListener applicationLifecycleListener = new ApplicationLifecycleListener(mockHandler);
        Activity mockActivity = mock(Activity.class);
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Start activity again.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Stop.
        applicationLifecycleListener.onActivityStopped(mockActivity);
        applicationLifecycleListener.onActivityStopped(mockActivity);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1, times(2)).onApplicationStarted();
        verify(mockAppCenterService2, times(2)).onApplicationStarted();
    }

    @Test
    public void onActivityStartAfterResumeTest() {

        // Prepare data.
        Handler mockHandler = mock(Handler.class);
        ApplicationLifecycleListener applicationLifecycleListener = new ApplicationLifecycleListener(mockHandler);
        Activity mockActivity = mock(Activity.class);
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Resume.
        applicationLifecycleListener.onActivityResumed(mockActivity);
        verify(mockHandler, times(0)).removeCallbacks(any(Runnable.class));

        // Stop.
        applicationLifecycleListener.onActivityStopped(mockActivity);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();
    }

    @Test
    public void onExpectedLifecycleTest() {

        // Prepare data.
        Handler mockHandler = mock(Handler.class);
        ApplicationLifecycleListener applicationLifecycleListener = new ApplicationLifecycleListener(mockHandler);
        Activity mockActivity = mock(Activity.class);
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        applicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Resume.
        applicationLifecycleListener.onActivityResumed(mockActivity);
        verify(mockHandler, times(0)).removeCallbacks(any(Runnable.class));

        // Pause.
        applicationLifecycleListener.onActivityPaused(mockActivity);
        verify(mockHandler).postDelayed(any(Runnable.class), anyLong());

        // Stop.
        applicationLifecycleListener.onActivityStopped(mockActivity);

        // Start.
        applicationLifecycleListener.onActivityStarted(mockActivity);
        verify(mockAppCenterService1, times(1)).onApplicationStarted();
        verify(mockAppCenterService2, times(1)).onApplicationStarted();
    }

    private class MockCallbacks implements ApplicationLifecycleListener.ApplicationLifecycleCallbacks {

        @Override
        public void onApplicationStarted() {

        }

        @Override
        public void onApplicationStopped() {

        }
    }
}
