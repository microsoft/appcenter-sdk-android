package avalanche.crash.model;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import avalanche.base.Constants;
import avalanche.base.utils.AvalancheLog;

import static avalanche.base.utils.StorageHelper.InternalStorage;

public class CrashReport implements Serializable {

    private static final String FIELD_FORMAT_VALUE = "Xamarin";
    private static final String FIELD_XAMARIN_CAUSED_BY = "Xamarin caused by: "; //Field that marks a Xamarin Exception

    private final String crashIdentifier;

    private String reporterKey;

    private Date appStartDate;
    private Date appCrashDate;

    private String osVersion;
    private String osBuild;
    private String deviceManufacturer;
    private String deviceModel;

    private String appPackage;
    private String appVersionName;
    private String appVersionCode;

    private String threadName;

    private String throwableStackTrace;

    private Boolean isXamarinException;

    private String format;

    public CrashReport(String crashIdentifier) {
        this.crashIdentifier = crashIdentifier;
    }

    public CrashReport(String crashIdentifier, Throwable throwable) {
        this(crashIdentifier);

        isXamarinException = false;

        final Writer stackTraceResult = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stackTraceResult);
        throwable.printStackTrace(printWriter);
        throwableStackTrace = stackTraceResult.toString();
    }

    public CrashReport(String crashIdentifier, Throwable throwable, String managedExceptionString, Boolean isManagedException) {
        this(crashIdentifier);

        final Writer stackTraceResult = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stackTraceResult);

        isXamarinException = true;

        //Add the header field "Format" to the crash
        //the value is "Xamarin", for now there are no other values and it's only set in case we have an exception coming from
        //the Xamarin SDK. It can be a java exception, a managed exception, or a mixed one.
        setFormat(FIELD_FORMAT_VALUE);

        if (isManagedException) {
            //add "Xamarin Caused By" before the managed stacktrace. No new line after it.
            printWriter.print(FIELD_XAMARIN_CAUSED_BY);

            //print the managed exception
            throwable.printStackTrace(printWriter);
        } else {
            //If we have managedExceptionString, we hava a MIXED (Java & C#)
            //exception, The throwable will be the Java exception.
            if (!TextUtils.isEmpty(managedExceptionString)) {
                //Print the java exception
                throwable.printStackTrace(printWriter);

                //Add "Xamarin Caused By" before the managed stacktrace. No new line after it.
                printWriter.print(FIELD_XAMARIN_CAUSED_BY);
                //print the stacktrace of the managed exception
                printWriter.print(managedExceptionString);
            } else {
                //we have a java exception, no "Xamarin Caused By:"
                throwable.printStackTrace(printWriter);
            }
        }

        throwableStackTrace = stackTraceResult.toString();
    }

    public static CrashReport fromFile(File file) throws IOException, ClassNotFoundException {
        return InternalStorage.readObject(file);
    }

    public void writeCrashReport() {
        String path = Constants.FILES_PATH + "/" + crashIdentifier + ".stacktrace";
        AvalancheLog.debug("Writing unhandled exception to: " + path);

        try {
            InternalStorage.writeObject(new File(path), this);
        } catch (IOException e) {
            AvalancheLog.error("Error saving crash report!", e);
        }
    }

    public String getCrashIdentifier() {
        return crashIdentifier;
    }

    public String getReporterKey() {
        return reporterKey;
    }

    public void setReporterKey(String reporterKey) {
        this.reporterKey = reporterKey;
    }

    public Date getAppStartDate() {
        return appStartDate;
    }

    public void setAppStartDate(Date appStartDate) {
        this.appStartDate = appStartDate;
    }

    public Date getAppCrashDate() {
        return appCrashDate;
    }

    public void setAppCrashDate(Date appCrashDate) {
        this.appCrashDate = appCrashDate;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getOsBuild() {
        return osBuild;
    }

    public void setOsBuild(String osBuild) {
        this.osBuild = osBuild;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public void setAppVersionName(String appVersionName) {
        this.appVersionName = appVersionName;
    }

    public String getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(String appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getThrowableStackTrace() {
        return throwableStackTrace;
    }

    public void setThrowableStackTrace(String throwableStackTrace) {
        this.throwableStackTrace = throwableStackTrace;
    }

    public Boolean getIsXamarinException() {
        return isXamarinException;
    }

    public void setIsXamarinException(Boolean isXamarinException) {
        this.isXamarinException = isXamarinException;
    }

    //We could to without a Format property and getters/setters, but we will eventually use this
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
