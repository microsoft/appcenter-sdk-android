package com.microsoft.appcenter;

import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.ShutdownHelper;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Uncaught exception handler of core module, to wait storage operations to finish on crash.
 */
class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    /**
     * Shutdown timeout in millis.
     */
    private static final int SHUTDOWN_TIMEOUT = 5000;

    /**
     * Handler on App Center background thread.
     */
    private final Handler mHandler;

    /**
     * App Center channel.
     */
    private final Channel mChannel;

    /**
     * Default/previous exception handler for chaining calls.
     */
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    /**
     * Init.
     *
     * @param handler handler.
     * @param channel channel.
     */
    UncaughtExceptionHandler(Handler handler, Channel channel) {
        mHandler = handler;
        mChannel = channel;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (AppCenter.getInstance().isInstanceEnabled()) {

            /* Wait channel to finish saving other logs in background. */
            final Semaphore semaphore = new Semaphore(0);
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mChannel.shutdown();
                    AppCenterLog.debug(AppCenter.LOG_TAG, "Channel completed shutdown.");
                    semaphore.release();
                }
            });
            try {
                if (!semaphore.tryAcquire(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    AppCenterLog.error(AppCenter.LOG_TAG, "Timeout waiting for looper tasks to complete.");
                }
            } catch (InterruptedException e) {
                AppCenterLog.warn(AppCenter.LOG_TAG, "Interrupted while waiting looper to flush.", e);
            }
        }
        if (mDefaultUncaughtExceptionHandler != null) {
            mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
        } else {
            ShutdownHelper.shutdown(10);
        }
    }

    @VisibleForTesting
    Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return mDefaultUncaughtExceptionHandler;
    }

    /**
     * Register uncaught exception handler.
     */
    void register() {
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Unregister uncaught exception handler.
     */
    void unregister() {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
    }
}
