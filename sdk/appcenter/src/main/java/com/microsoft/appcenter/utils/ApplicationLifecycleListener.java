package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ApplicationLifecycleListener implements ActivityLifecycleCallbacks {

    /**
     * The timeout used to determine an actual transition to the background.
     */
    private static final long TIMEOUT_MS = 700;

    private int mStartedCounter = 0;
    private int mResumedCounter = 0;
    private boolean mPauseSent = true;
    private boolean mStopSent = true;

    private Handler mHandler;

    private List<ApplicationLifecycleCallbacks> mLifecycleCallbacks = new ArrayList<>();

    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    public ApplicationLifecycleListener(Handler handler) {
        this.mHandler = handler;
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
                service.onApplicationStopped();
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
                service.onApplicationStarted();
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
        void onApplicationStarted();

        void onApplicationStopped();
    }
}
