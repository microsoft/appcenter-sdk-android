package com.microsoft.azure.mobile.push;

import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Push notifications interface
 */
public class Push extends AbstractMobileCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Push";

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Init.
     */
    private Push() {
        mFactories = new HashMap<>();
        mFactories.put(PushInstallationLog.TYPE, new PushInstallationLogFactory());
    }

    @Override
    protected String getGroupName() {
        return null;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }
}
