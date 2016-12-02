/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.crashes;

import android.os.Process;
import android.support.annotation.VisibleForTesting;

class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private boolean mIgnoreDefaultExceptionHandler = false;

    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (!Crashes.isEnabled()) {
            if (mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            }
        } else {
            Crashes.getInstance().saveUncaughtException(thread, exception);
            if (mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                ShutdownHelper.shutdown();
            }
        }
    }

    @VisibleForTesting
    void setIgnoreDefaultExceptionHandler(boolean ignoreDefaultExceptionHandler) {
        mIgnoreDefaultExceptionHandler = ignoreDefaultExceptionHandler;
        if (ignoreDefaultExceptionHandler) {
            mDefaultUncaughtExceptionHandler = null;
        }
    }

    @VisibleForTesting
    Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return mDefaultUncaughtExceptionHandler;
    }

    void register() {
        if (!mIgnoreDefaultExceptionHandler) {
            mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        } else {
            mDefaultUncaughtExceptionHandler = null;
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    void unregister() {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
    }

    @VisibleForTesting
    static class ShutdownHelper {

        static void shutdown() {
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }
}
