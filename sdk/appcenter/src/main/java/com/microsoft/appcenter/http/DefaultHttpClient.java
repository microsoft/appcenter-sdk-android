/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Default HTTP client without the additional behaviors.
 */
public class DefaultHttpClient implements HttpClient, DefaultHttpClientCallTask.Tracker {

    /**
     * HTTP GET method.
     */
    public static final String METHOD_GET = "GET";

    /**
     * HTTP POST method.
     */
    public static final String METHOD_POST = "POST";

    /**
     * HTTP DELETE method.
     */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * Retry after milliseconds duration header.
     */
    static final String X_MS_RETRY_AFTER_MS_HEADER = "x-ms-retry-after-ms";

    /**
     * Content type header key.
     */
    public static final String CONTENT_TYPE_KEY = "Content-Type";

    /**
     * Content type header value.
     */
    static final String CONTENT_TYPE_VALUE = "application/json";

    /**
     * Character encoding.
     */
    static final String CHARSET_NAME = "UTF-8";

    /**
     * Content encoding header key.
     */
    static final String CONTENT_ENCODING_KEY = "Content-Encoding";

    /**
     * Content encoding header key.
     */
    static final String CONTENT_ENCODING_VALUE = "gzip";

    /**
     * List of ongoing call tasks.
     */
    private final Set<DefaultHttpClientCallTask> mTasks = new HashSet<>();

    /**
     * Indicates whether compression is enabled.
     */
    private final boolean mCompressionEnabled;

    public DefaultHttpClient() {
        this(true);
    }

    public DefaultHttpClient(boolean compressionEnabled) {
        mCompressionEnabled = compressionEnabled;
    }

    @VisibleForTesting
    Set<DefaultHttpClientCallTask> getTasks() {
        return mTasks;
    }

    @Override
    public ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, final ServiceCallback serviceCallback) {
        final DefaultHttpClientCallTask task = new DefaultHttpClientCallTask(url, method, headers, callTemplate, serviceCallback, this, mCompressionEnabled);
        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final RejectedExecutionException e) {

            /*
             * When executor saturated (shared with app), we should use the retry mechanism
             * rather than creating more threads to avoid putting too much pressure on the hosting app.
             * Also we need to return the method before calling the listener,
             * so we post the callback on handler to make sure of that.
             */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    serviceCallback.onCallFailed(e);
                }
            });
        }
        return new ServiceCall() {

            @Override
            public void cancel() {

                /* This doesn't kill the AsyncTask, so we should check the state manually. */
                task.cancel(true);
            }
        };
    }

    @Override
    public synchronized void onStart(DefaultHttpClientCallTask task) {
        mTasks.add(task);
    }

    @Override
    public synchronized void onFinish(DefaultHttpClientCallTask task) {
        mTasks.remove(task);
    }

    @Override
    public synchronized void close() {
        if (mTasks.size() > 0) {
            AppCenterLog.debug(LOG_TAG, "Cancelling " + mTasks.size() + " network call(s).");
            for (DefaultHttpClientCallTask task : mTasks) {
                task.cancel(true);
            }
            mTasks.clear();
        }
    }

    @Override
    public void reopen() {

        /* Nothing to do. */
    }

    @VisibleForTesting
    boolean isCompressionEnabled() {
        return mCompressionEnabled;
    }
}
