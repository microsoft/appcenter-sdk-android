/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Listens to the whole application (if the application is a single process) lifecycle events.
 * It allows catching of a foregrounded and a backgrounded state of the whole application.
 * It won't fire any events when there's a configuration change in a foregrounded app or if
 * user goes through activities inside a single application.
 */
public class ApplicationLifecycleListener implements ActivityLifecycleCallbacks {

    /**
     * The timeout used to determine an actual transition to the background.
     */
    private static final long TIMEOUT_MS = 700;

    /**
     * Counter of onActivityStarted() minus onActivityStopped() events.
     */
    private int mStartedCounter = 0;

    /**
     * Counter of onActivityResumed() minus onActivityPaused() events.
     */
    private int mResumedCounter = 0;

    /**
     * Flag indicating that the last activity is paused.
     */
    private boolean mPauseSent = true;

    /**
     * Flag indicating that the last activity is stopped.
     */
    private boolean mStopSent = true;

    /**
     * Background thread handler.
     */
    private Handler mHandler;

    /**
     * A set of ApplicationLifecycleCallbacks.
     */
    private final Set<ApplicationLifecycleCallbacks> mLifecycleCallbacks = new CopyOnWriteArraySet<>();

    private Runnable mDelayedPauseRunnable = new Runnable() {

        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    public ApplicationLifecycleListener(Handler handler) {
        mHandler = handler;
    }

    public void registerApplicationLifecycleCallbacks(ApplicationLifecycleCallbacks lifecycleCallback) {
        mLifecycleCallbacks.add(lifecycleCallback);
    }

    private void dispatchPauseIfNeeded() {
        if (mResumedCounter == 0) {
            mPauseSent = true;
        }
    }

    private void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
            for (ApplicationLifecycleCallbacks service : mLifecycleCallbacks) {
                service.onApplicationEnterBackground();
            }
            mStopSent = true;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            for (ApplicationLifecycleCallbacks service : mLifecycleCallbacks) {
                service.onApplicationEnterForeground();
            }
            mStopSent = false;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mResumedCounter++;
        if (mResumedCounter == 1) {
            if (mPauseSent) {
                mPauseSent = false;
            } else {
                mHandler.removeCallbacks(mDelayedPauseRunnable);
            }
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        mResumedCounter--;
        if (mResumedCounter == 0) {

            /*
             * OnPause and onStop events will be dispatched with a delay after a last activity
             * passed through them. This delay is long enough to guarantee that
             * ApplicationLifecycleListener won't send any events if activities are destroyed
             * and recreated due to a configuration change.
             */
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mStartedCounter--;
        dispatchStopIfNeeded();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    public interface ApplicationLifecycleCallbacks {
        void onApplicationEnterForeground();

        void onApplicationEnterBackground();
    }
}
