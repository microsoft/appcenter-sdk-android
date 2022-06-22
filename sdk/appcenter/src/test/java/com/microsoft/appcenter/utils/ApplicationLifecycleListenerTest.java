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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationLifecycleListenerTest {

    @Mock
    private Handler mHandlerMock;

    @Mock
    private Activity mActivityMock;

    private ApplicationLifecycleListener mApplicationLifecycleListener;

    @Before
    public void setUp() {
        mApplicationLifecycleListener = new ApplicationLifecycleListener(mHandlerMock);
    }

    @Test
    public void activityResumesBeforeDelayedCallback() {

        /* Prepare data. */
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks);

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityResumed again. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Pause. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        verify(mHandlerMock, never()).postDelayed(any(Runnable.class), anyLong());

        /* Call onActivityPaused again. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        ArgumentCaptor<Runnable> postDelayed = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandlerMock).postDelayed(postDelayed.capture(), anyLong());

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock).removeCallbacks(any(Runnable.class));

        /* Runnable from onActivityPaused is called after the timeout. */
        postDelayed.getValue().run();

        /* Verify that enter background callback won't call after the delay. */
        verify(callbacks, never()).onApplicationEnterBackground();
    }

    @Test
    public void activityStartsTwice() {

        /* Prepare data. */
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks1 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks2 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();

        /* Call onActivityStarted again. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStopped again. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1, times(2)).onApplicationEnterForeground();
        verify(callbacks2, times(2)).onApplicationEnterForeground();
    }

    @Test
    public void activityStartsAfterResume() {

        /* Prepare data. */
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks1 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks2 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();
    }

    @Test
    public void expectedLifecycle() {

        /* Prepare data. */
        Bundle mockBundle = mock(Bundle.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks1 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks2 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityCreated(mActivityMock, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        ArgumentCaptor<Runnable> postDelayedRunnable = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandlerMock).postDelayed(postDelayedRunnable.capture(), anyLong());

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);
        mApplicationLifecycleListener.onActivitySaveInstanceState(mActivityMock, mockBundle);

        /* Runnable from onActivityPaused is called after the timeout. */
        postDelayedRunnable.getValue().run();

        /* Verify background callbacks called once. */
        verify(callbacks1).onApplicationEnterBackground();
        verify(callbacks2).onApplicationEnterBackground();

        /* Call onActivityDestroyed. */
        mApplicationLifecycleListener.onActivityDestroyed(mActivityMock);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Verify background callbacks still were called once. */
        verify(callbacks1).onApplicationEnterBackground();
        verify(callbacks2).onApplicationEnterBackground();

        /* Verify foreground callbacks were called twice. */
        verify(callbacks1, times(2)).onApplicationEnterForeground();
        verify(callbacks2, times(2)).onApplicationEnterForeground();
    }

    @Test
    public void activityTransitionTest() {

        /* Prepare data. */
        Activity anotherActivityMock = mock(Activity.class);
        Bundle mockBundle = mock(Bundle.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks1 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks2 = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks1);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks2);

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityCreated(mActivityMock, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks1).onApplicationEnterForeground();
        verify(callbacks2).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));
        mApplicationLifecycleListener.onActivitySaveInstanceState(mActivityMock, mockBundle);

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        ArgumentCaptor<Runnable> postDelayed = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandlerMock).postDelayed(postDelayed.capture(), anyLong());

        /* Mimic behavior of ActivityManager when a user opens another activity. */
        mApplicationLifecycleListener.onActivityCreated(anotherActivityMock, mockBundle);
        mApplicationLifecycleListener.onActivityStarted(anotherActivityMock);
        mApplicationLifecycleListener.onActivityResumed(anotherActivityMock);

        /* Verify the runnable was removed in onActivityResumed. */
        verify(mHandlerMock).removeCallbacks(any(Runnable.class));

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Verify that delayed callback doesn't trigger enter background callbacks. */
        postDelayed.getValue().run();
        verify(callbacks1, never()).onApplicationEnterBackground();
        verify(callbacks2, never()).onApplicationEnterBackground();

        /* Verify the callback was not called if we transition to another activity inside the app. */
        verifyNoMoreInteractions(callbacks1, callbacks2);
    }

    @Test
    public void skipCallOnStartOpenFoldOpen() {

        /* Prepare data. */
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks);

        /* Skip call onActivityStarted. */

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        ArgumentCaptor<Runnable> postDelayedRunnable = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandlerMock).postDelayed(postDelayedRunnable.capture(), anyLong());

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Runnable from onActivityPaused is called after the timeout. */
        postDelayedRunnable.getValue().run();

        /* Verify onApplicationEnterBackground was called only once. */
        verify(callbacks).onApplicationEnterBackground();

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void skipCallOnResumeOpenFoldOpen() {

        /* Prepare data. */
        ApplicationLifecycleListener.ApplicationLifecycleCallbacks callbacks = mock(ApplicationLifecycleListener.ApplicationLifecycleCallbacks.class);
        mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(callbacks);

        /* Skip call onActivityStarted and onActivityResume. */

        /* Call onActivityPaused. */
        mApplicationLifecycleListener.onActivityPaused(mActivityMock);
        ArgumentCaptor<Runnable> postDelayedRunnable = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandlerMock).postDelayed(postDelayedRunnable.capture(), anyLong());

        /* Call onActivityStopped. */
        mApplicationLifecycleListener.onActivityStopped(mActivityMock);

        /* Runnable for onActivityPaused is called after the timeout. */
        postDelayedRunnable.getValue().run();

        /* Verify onApplicationEnterBackground called. */
        verify(callbacks).onApplicationEnterBackground();

        /* Call onActivityStarted. */
        mApplicationLifecycleListener.onActivityStarted(mActivityMock);
        verify(callbacks).onApplicationEnterForeground();

        /* Call onActivityResumed. */
        mApplicationLifecycleListener.onActivityResumed(mActivityMock);
        verify(mHandlerMock, never()).removeCallbacks(any(Runnable.class));

        /* Verify the callbacks were called only once. */
        verify(callbacks).onApplicationEnterBackground();
        verify(callbacks).onApplicationEnterForeground();
    }
}
