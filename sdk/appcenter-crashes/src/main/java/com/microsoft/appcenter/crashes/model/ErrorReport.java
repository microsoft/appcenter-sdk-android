/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.model;

import com.microsoft.appcenter.ingestion.models.Device;

import java.util.Date;

/**
 * Error report class.
 */
public class ErrorReport {

    /**
     * UUID for crash report.
     */
    private String id;

    /**
     * Thread name that triggered the crash.
     */
    private String threadName;

    /**
     * The throwable that caused the crash.
     */
    private Throwable throwable;

    /**
     * The date and time the application started, <code>null</code> if unknown.
     */
    private Date appStartTime;

    /**
     * The date and time the crash occurred, <code>null</code> if unknown.
     */
    private Date appErrorTime;

    /**
     * The device information.
     */
    private Device device;

    /**
     * Gets the UUID for crash report.
     *
     * @return The UUID for crash report.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the UUID for crash report.
     *
     * @param id A UUID for crash report to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the thread name.
     *
     * @return The thread name.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Sets the thread name.
     *
     * @param threadName A thread name to set.
     */
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * Gets the throwable.
     *
     * @return The throwable.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Sets the throwable.
     *
     * @param throwable A throwable to set.
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Gets the application start datetime.
     *
     * @return The application start datetime.
     */
    public Date getAppStartTime() {
        return appStartTime;
    }

    /**
     * Sets the application start datetime.
     *
     * @param appStartTime An application start datetime to set.
     */
    public void setAppStartTime(Date appStartTime) {
        this.appStartTime = appStartTime;
    }

    /**
     * Gets the application error datetime.
     *
     * @return The application error datetime.
     */
    public Date getAppErrorTime() {
        return appErrorTime;
    }

    /**
     * Sets the application error datetime.
     *
     * @param appErrorTime An application error datetime to set.
     */
    public void setAppErrorTime(Date appErrorTime) {
        this.appErrorTime = appErrorTime;
    }

    /**
     * Gets the device information.
     *
     * @return The device information.
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Sets the device information.
     *
     * @param device A device information to set.
     */
    public void setDevice(Device device) {
        this.device = device;
    }
}
