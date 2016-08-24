package com.microsoft.sonoma.errors.model;

import com.microsoft.sonoma.core.ingestion.models.Device;

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
     */
    public void setDevice(Device device) {
        this.device = device;
    }
}
