package avalanche.errors.model;

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
     * The operation system version string the app was running on when it crashed.
     */
    private String osVersion;

    /**
     * The operation system build string the app was running on when it crashed. This may be unavailable (<code>null</code>).
     */
    private String osBuild;

    /**
     * The device manufacturer.
     */
    private String deviceManufacturer;

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
     * Gets the operation system version.
     *
     * @return The operation system version.
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Sets the operation system version.
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Gets the operation system build.
     *
     * @return The operation system build.
     */
    public String getOsBuild() {
        return osBuild;
    }

    /**
     * Sets the operation system build.
     */
    public void setOsBuild(String osBuild) {
        this.osBuild = osBuild;
    }

    /**
     * Gets the device manufacturer.
     *
     * @return The device manufacturer.
     */
    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    /**
     * Sets the device manufacturer.
     */
    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }
}
