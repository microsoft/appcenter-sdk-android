package com.microsoft.azure.mobile;

import android.os.AsyncTask;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.concurrent.RejectedExecutionException;

/**
 * AsyncTask utilities.
 */
public class AsyncTaskUtils {

    /**
     * Execute a task using {@link AsyncTask#THREAD_POOL_EXECUTOR} and fall back
     * using {@link AsyncTask#SERIAL_EXECUTOR} in case of {@link RejectedExecutionException}.
     *
     * @param logTag     log tag to use for logging a warning about the fallback.
     * @param asyncTask  task to execute.
     * @param params     parameters.
     * @param <Params>   parameters type.
     * @param <Progress> progress type.
     * @param <Result>   result type.
     * @return the task.
     */
    @SafeVarargs
    public static <Params, Progress, Result> AsyncTask<Params, Progress, Result> execute(String logTag, AsyncTask<Params, Progress, Result> asyncTask, Params... params) {
        try {
            return asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } catch (RejectedExecutionException e) {
            MobileCenterLog.warn(logTag, "THREAD_POOL_EXECUTOR saturated, fall back on SERIAL_EXECUTOR", e);
            return asyncTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, params);
        }
    }
}
