package com.microsoft.appcenter.sasquatch.listeners;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.widget.Toast;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.ingestion.models.LogWithProperties;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.sasquatch.R;

import org.json.JSONObject;

public class SasquatchAnalyticsListener implements com.microsoft.appcenter.analytics.channel.AnalyticsListener {

    @VisibleForTesting
    public static final CountingIdlingResource analyticsIdlingResource = new CountingIdlingResource("analytics");
    private final Context mContext;

    public SasquatchAnalyticsListener(Context context) {
        this.mContext = context;
    }

    @Override
    public void onBeforeSending(com.microsoft.appcenter.ingestion.models.Log log) {
        if (log instanceof EventLog || log instanceof CommonSchemaLog) {
            Toast.makeText(mContext, R.string.event_before_sending, Toast.LENGTH_SHORT).show();
        } else if (log instanceof PageLog) {
            Toast.makeText(mContext, R.string.page_before_sending, Toast.LENGTH_SHORT).show();
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
            message = String.format("%s\nException: %s", message, e.toString());
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
        analyticsIdlingResource.decrement();
    }
}
