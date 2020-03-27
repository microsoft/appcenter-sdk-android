/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
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

    private Handler mHandlerMock;
    private ApplicationLifecycleListener mApplicationLifecycleListenerMock;
    private Activity mActivityMock;
    private Bundle mBundleMock;

    @Before
    public void setUp() {
        mHandlerMock = mock(Handler.class);
        mApplicationLifecycleListenerMock = new ApplicationLifecycleListener(mHandlerMock);
        mActivityMock = mock(Activity.class);
    }

    @After
    public void tearDown() {
        mHandlerMock = null;
        mApplicationLifecycleListenerMock = null;
        mActivityMock = null;
    }

    @Test
    public void onActivityDoubleResumedTest() {

        // Prepare data.
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, times(0)).removeCallbacks(any(Runnable.class));

        // Call ActivityResume again.
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, times(0)).removeCallbacks(any(Runnable.class));

        // Pause.
        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock, times(0)).postDelayed(any(Runnable.class), anyLong());

        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        // Start.
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void onActivityDoubleStartTest() {

        // Prepare data.
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Start activity again.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Stop.
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1, times(2)).onApplicationStarted();
        verify(mockAppCenterService2, times(2)).onApplicationStarted();
    }

    @Test
    public void onActivityStartAfterResumeTest() {

        // Prepare data.
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        // Resume.
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, times(0)).removeCallbacks(any(Runnable.class));

        // Stop.
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();
    }

    @Test
    public void onExpectedLifecycleTest() {

        // Prepare data.
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();
        mApplicationLifecycleListenerMock.onActivityCreated(mActivityMock, mBundleMock);

        // Resume.
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, times(0)).removeCallbacks(any(Runnable.class));
        mApplicationLifecycleListenerMock.onActivitySaveInstanceState(mActivityMock, mBundleMock);

        // Pause.
        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        // Stop.
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        mApplicationLifecycleListenerMock.onActivityDestroyed(mActivityMock);

        // Start.
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
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
