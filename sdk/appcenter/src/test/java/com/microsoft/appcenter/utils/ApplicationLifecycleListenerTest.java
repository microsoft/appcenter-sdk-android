/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.microsoft.appcenter.AbstractAppCenterService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class ApplicationLifecycleListenerTest {

    @Mock
    Handler mHandlerMock;

    @Mock
    Activity mActivityMock;

    private ApplicationLifecycleListener mApplicationLifecycleListener;

    @Before
    public void setUp() {
        mApplicationLifecycleListener = new ApplicationLifecycleListener(mHandlerMock);
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
        MockService mockAppCenterService1 = spy(new MockService());
        MockService mockAppCenterService2 = spy(new MockService());
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityResumed again. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Pause.*/
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        verify(mHandlerMock, never()).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityPaused again. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void onActivityDoubleStartTest() {

        /* Prepare data. */
        MockService mockAppCenterService1 = spy(new MockService());
        MockService mockAppCenterService2 = spy(new MockService());
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();

        /* Call onActivityStarted again. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStopped again. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1, times(2)).onApplicationEnterForeground();
        verify(mockAppCenterService2, times(2)).onApplicationEnterForeground();
    }

    @Test
    public void onActivityStartAfterResumeTest() {

        /* Prepare data. */
        MockService mockAppCenterService1 = spy(new MockService());
        MockService mockAppCenterService2 = spy(new MockService());
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();
    }

    @Test
    public void onExpectedLifecycleTest() {

        /* Prepare data. */
        Bundle mockBundle = mock(Bundle.class);
        MockService mockAppCenterService1 = spy(new MockService());
        MockService mockAppCenterService2 = spy(new MockService());
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityCreated(mActivityMock, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));
        mApplicationLifecycleListener.onActivitySaveInstanceState(mActivityMock, mockBundle);

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityDestroyed. */
        mApplicationLifecycleListener.onActivityDestroyed(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1, times(2)).onApplicationEnterForeground();
        verify(mockAppCenterService2, times(2)).onApplicationEnterForeground();
    }

    @Test
    public void activityTransitionTest() {

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
        Activity activityMock2 = mock(Activity.class);
        Bundle mockBundle = mock(Bundle.class);
        MockService mockAppCenterService1 = spy(new MockService());
        MockService mockAppCenterService2 = spy(new MockService());
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(mockAppCenterService2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityCreated(mActivityMock, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(mockAppCenterService1).onApplicationEnterForeground();
        verify(mockAppCenterService2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));
        mApplicationLifecycleListener.onActivitySaveInstanceState(mActivityMock, mockBundle);

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        verify(mHandlerMock).postDelayed(any(Runnable.class), anyLong());

        /* Mimic behavior of ActivityManager when a user opens another activity. */
        mApplicationLifecycleListener.onActivityCreated(activityMock2, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(activityMock2);
        mApplicationLifecycleListener.onActivityResumed(activityMock2);

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Verify the callback was not called if we transition to another activity inside the app. */
        verify(mockAppCenterService1, times(1)).onApplicationEnterForeground();
        verify(mockAppCenterService2, times(1)).onApplicationEnterForeground();
    }

    private class MockService extends AbstractAppCenterService {

        @Override
        protected String getGroupName() {
            return null;
        }

        @Override
        protected String getLoggerTag() {
            return null;
        }

        @Override
        public String getServiceName() {
            return null;
        }
    }
}
