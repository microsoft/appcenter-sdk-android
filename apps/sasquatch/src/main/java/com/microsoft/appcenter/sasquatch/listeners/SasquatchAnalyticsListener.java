/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.listeners;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.widget.Toast;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.ingestion.models.LogWithProperties;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.HandlerUtils;

import org.json.JSONObject;

public class SasquatchAnalyticsListener implements com.microsoft.appcenter.analytics.channel.AnalyticsListener {

    @VisibleForTesting
    public static final CountingIdlingResource analyticsIdlingResource = new CountingIdlingResource("analytics");

    private final Context mContext;

    private static final long TOAST_DELAY = 2000;

    private long mLastToastTime;

    private long mPendingLogCount;

    public SasquatchAnalyticsListener(Context context) {
        this.mContext = context;
    }

    @Override
    public void onBeforeSending(com.microsoft.appcenter.ingestion.models.Log log) {
        if (log instanceof EventLog || log instanceof CommonSchemaLog) {
            notifyBeforeSending(mContext.getString(R.string.event_before_sending));
        } else if (log instanceof PageLog) {
            notifyBeforeSending(mContext.getString(R.string.page_before_sending));
        }
        analyticsIdlingResource.increment();
    }

    @Override
    public void onSendingFailed(com.microsoft.appcenter.ingestion.models.Log log, Exception e) {
        String message = null;
        if (log instanceof EventLog || log instanceof CommonSchemaLog) {
            message = mContext.getString(R.string.event_sent_failed);
        } else if (log instanceof PageLog) {
            message = mContext.getString(R.string.page_sent_failed);
        }
        if (message != null) {
            notifySendingCompletion(String.format("%s\nException: %s", message, e.toString()));
        }
        analyticsIdlingResource.decrement();
    }

    @Override
    public void onSendingSucceeded(com.microsoft.appcenter.ingestion.models.Log log) {
        String message = null;
        if (log instanceof EventLog) {
            message = String.format("%s\nName: %s", mContext.getString(R.string.event_sent_succeeded), ((EventLog) log).getName());
        } else if (log instanceof PageLog) {
            message = String.format("%s\nName: %s", mContext.getString(R.string.page_sent_succeeded), ((PageLog) log).getName());
        } else if (log instanceof CommonSchemaLog) {
            CommonSchemaLog commonSchemaLog = (CommonSchemaLog) log;
            message = String.format("%s\nName: %s", mContext.getString(R.string.event_sent_succeeded), commonSchemaLog.getName());
            if (commonSchemaLog.getData() != null) {
                message += String.format("\nProperties: %s", commonSchemaLog.getData().getProperties().toString());
            }
        }
        if (log instanceof LogWithProperties) {
            if (((LogWithProperties) log).getProperties() != null) {
                message += String.format("\nProperties: %s", new JSONObject(((LogWithProperties) log).getProperties()).toString());
            }
        }
        if (message != null) {
            notifySendingCompletion(message);
        }
        analyticsIdlingResource.decrement();
    }

    private void notifyBeforeSending(String message) {
        if (mPendingLogCount++ == 0) {
            showOrDelayToast(message);
        }
    }

    private void notifySendingCompletion(String message) {
        if (--mPendingLogCount == 0) {
            showOrDelayToast(message);
        }
    }

    private void showOrDelayToast(final String message) {
        long now = SystemClock.uptimeMillis();
        long timeToWait = mLastToastTime + TOAST_DELAY - now;
        if (timeToWait <= 0) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            mLastToastTime = now;
        } else {
            mLastToastTime = now + timeToWait;
            HandlerUtils.getMainHandler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            }, timeToWait);
        }
    }

}
