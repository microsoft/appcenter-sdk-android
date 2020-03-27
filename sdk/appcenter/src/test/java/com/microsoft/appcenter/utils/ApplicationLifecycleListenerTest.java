/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class ApplicationLifecycleListenerTest {

    @Mock
    Handler mHandlerMock;

    @Mock
    Activity mActivityMock;

    private ApplicationLifecycleListener mApplicationLifecycleListenerMock;

    @Before
    public void setUp() {
        mApplicationLifecycleListenerMock = new ApplicationLifecycleListener(mHandlerMock);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mHandlerMock).postDelayed(any(Runnable.class), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mHandlerMock).removeCallbacks(any(Runnable.class));
    }
    
    @Test
    public void onActivityDoubleResumedTest() {

        /* Prepare data. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(final InvocationOnMock invocation) {

                /* Post with a small delay to ensure that application resumes first. */
                new Timer().schedule(new TimerTask() {

                    @Override
                    public void run() {
                        ((Runnable) invocation.getArguments()[0]).run();
                    }
                }, 100);
                return null;
            }
        }).when(mHandlerMock).postDelayed(any(Runnable.class), anyLong());
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityResumed. */
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityResumed again. */
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Pause.*/
        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock, never()).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityPaused again. */
        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityResumed. */
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void onActivityDoubleStartTest() {

        /* Prepare data. */
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        /* Call onActivityStarted again. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        /* Call onActivityStopped. */
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        /* Call onActivityStopped again. */
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1, times(2)).onApplicationStarted();
        verify(mockAppCenterService2, times(2)).onApplicationStarted();
    }

    @Test
    public void onActivityStartAfterResumeTest() {

        /* Prepare data. */
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();

        /* Call onActivityResumed. */
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityStopped. */
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();
    }

    @Test
    public void onExpectedLifecycleTest() {

        /* Prepare data. */
        Bundle mockBundle = mock(Bundle.class);
        MockCallbacks mockAppCenterService1 = mock(MockCallbacks.class);
        MockCallbacks mockAppCenterService2 = mock(MockCallbacks.class);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListenerMock.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationStarted();
        verify(mockAppCenterService2).onApplicationStarted();
        mApplicationLifecycleListenerMock.onActivityCreated(mActivityMock, mockBundle);

        /* Call onActivityResumed. */
        mApplicationLifecycleListenerMock.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));
        mApplicationLifecycleListenerMock.onActivitySaveInstanceState(mActivityMock, mockBundle);

        /* Call onActivityPaused. */
        mApplicationLifecycleListenerMock.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityStopped. */
        mApplicationLifecycleListenerMock.onActivityStopped(mActivityMock);

        /* Call onActivityDestroyed. */
        mApplicationLifecycleListenerMock.onActivityDestroyed(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListenerMock.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1, times(2)).onApplicationStarted();
        verify(mockAppCenterService2, times(2)).onApplicationStarted();
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
