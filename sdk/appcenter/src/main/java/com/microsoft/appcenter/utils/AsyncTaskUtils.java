/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.RejectedExecutionException;

/**
 * AsyncTask utilities.
 */
public class AsyncTaskUtils {

    @VisibleForTesting
    AsyncTaskUtils() {

        /* Hide constructor in utils. */
    }

    /**
     * Execute a task using {@link AsyncTask#THREAD_POOL_EXECUTOR} and fall back
     * using {@link AsyncTask#SERIAL_EXECUTOR} in case of {@link RejectedExecutionException}.
     *
     * @param logTag    log tag to use for logging a warning about the fallback.
     * @param asyncTask task to execute.
     * @param params    parameters.
     * @param <Params>  parameters type.
     * @param <Type>    task type.
     * @return the task.
     */
    @NonNull
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <Params, Type extends AsyncTask<Params, ?, ?>> Type execute(String logTag, @NonNull Type asyncTask, Params... params) {
        try {
            return (Type) asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } catch (RejectedExecutionException e) {
            AppCenterLog.warn(logTag, "THREAD_POOL_EXECUTOR saturated, fall back on SERIAL_EXECUTOR which has an unbounded queue", e);
            return (Type) asyncTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, params);
        }
    }
}
