/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.ingestion.models.Thread;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.context.UserIdContext;
import com.microsoft.appcenter.utils.storage.FileManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.Constants.WRAPPER_SDK_NAME_NDK;

/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 */
public class ErrorLogHelper {

    /**
     * Device info filename.
     */
    public static final String DEVICE_INFO_FILE = "deviceInfo";

    /**
     * Error log file extension for the JSON schema.
     */
    public static final String ERROR_LOG_FILE_EXTENSION = ".json";

    /**
     * Error log file extension for the serialized throwable for client side inspection.
     */
    public static final String THROWABLE_FILE_EXTENSION = ".throwable";

    /**
     * Directory under the FILES_PATH containing minidump files.
     */
    private static final String MINIDUMP_DIRECTORY = "minidump";

    /**
     * Directory under the MINIDUMP_DIRECTORY for new dump files.
     */
    private static final String NEW_MINIDUMP_DIRECTORY = "new";

    /**
     * Directory under the MINIDUMP_DIRECTORY for pending dump files.
     */
    private static final String PENDING_MINIDUMP_DIRECTORY = "pending";

    /**
     * For huge stack traces such as giant StackOverflowError, we keep only beginning and end of frames according to this limit.
     */
    @VisibleForTesting
    public static final int FRAME_LIMIT = 256;

    /**
     * We keep the first half of the limit of frames from the beginning and the second half from end.
     */
    private static final int FRAME_LIMIT_HALF = FRAME_LIMIT / 2;

    /**
     * For huge exception cause chains, we keep only beginning and end of causes according to this limit.
     */
    @VisibleForTesting
    static final int CAUSE_LIMIT = 16;

    /**
     * We keep the first half of the limit of causes from the beginning and the second half from end.
     */
    private static final int CAUSE_LIMIT_HALF = CAUSE_LIMIT / 2;

    /**
     * Error log directory within application files.
     */
    @VisibleForTesting
    static final String ERROR_DIRECTORY = "error";

    /**
     * Root directory for error log and throwable files.
     */
    private static File sErrorLogDirectory;

    /**
     * Max number of properties.
     */
    private static final int MAX_PROPERTY_COUNT = 20;

    /**
     * Max length of properties.
     */
    public static final int MAX_PROPERTY_ITEM_LENGTH = 125;

    /**
     * Minidump file extension for the NDK crashes.
     */
    public static final String MINIDUMP_FILE_EXTENSION = ".dmp";

    /**
     * Directory for new minidump files.
     */
    private static File sNewMinidumpDirectory;

    /**
     * Directory for pending minidump files.
     */
    private static File sPendingMinidumpDirectory;

    /**
     * Key for saving deviceInfo to JSON.
     */
    @VisibleForTesting
    static String DEVICE_INFO_KEY = "DEVICE_INFO";

    /**
     * Key for data residency region.
     */
    private static final String DATA_RESIDENCY_REGION_KEY = "dataResidencyRegion";

    /**
     * Key for saving userId to JSON.
     */
    @VisibleForTesting
    static String USER_ID_KEY = "USER_ID";

    @NonNull
    public static ManagedErrorLog createErrorLog(@NonNull Context context, @NonNull final java.lang.Thread thread, @NonNull final Throwable throwable, @NonNull final Map<java.lang.Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp) {
        return createErrorLog(context, thread, getModelExceptionFromThrowable(throwable), allStackTraces, initializeTimestamp, true);
    }

    @NonNull
    public static ManagedErrorLog createErrorLog(@NonNull Context context, @NonNull final java.lang.Thread thread, @NonNull final Exception exception, @NonNull final Map<java.lang.Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp, boolean fatal) {

        /* Build error log with a unique identifier. */
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUID.randomUUID());

        /* Set current time. Will be correlated to session after restart. */
        errorLog.setTimestamp(new Date());

        /* Set user identifier. */
        errorLog.setUserId(UserIdContext.getInstance().getUserId());

        /* Set data residency region. */
        errorLog.setDataResidencyRegion(AppCenter.getDataResidencyRegion());

        /* Snapshot device properties. */
        try {
            errorLog.setDevice(DeviceInfoHelper.getDeviceInfo(context));
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Could not attach device properties snapshot to error log, will attach at sending time", e);
        }

        /* Process information. Parent one is not available on Android. */
        errorLog.setProcessId(Process.myPid());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
            if (runningAppProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
                    if (info.pid == Process.myPid()) {
                        errorLog.setProcessName(info.processName);
                    }
                }
            }
        }

        /*
         * Process name is required field for crash processing but cannot always be available,
         * make sure we send a default value if not found.
         */
        if (errorLog.getProcessName() == null) {
            errorLog.setProcessName("");
        }

        /* CPU architecture. */
        errorLog.setArchitecture(getArchitecture());

        /* Thread in error information. */
        errorLog.setErrorThreadId(thread.getId());
        errorLog.setErrorThreadName(thread.getName());

        /* Uncaught exception or managed exception. */
        errorLog.setFatal(fatal);

        /* Application launch time. */
        errorLog.setAppLaunchTimestamp(new Date(initializeTimestamp));

        /* Attach exceptions. */
        errorLog.setException(exception);

        /* Attach thread states. */
        List<Thread> threads = new ArrayList<>(allStackTraces.size());
        for (Map.Entry<java.lang.Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread javaThread = new Thread();
            javaThread.setId(entry.getKey().getId());
            javaThread.setName(entry.getKey().getName());
            javaThread.setFrames(getModelFramesFromStackTrace(entry.getValue()));
            threads.add(javaThread);
        }
        errorLog.setThreads(threads);
        return errorLog;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getArchitecture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    @NonNull
    public static synchronized File getErrorStorageDirectory() {
        if (sErrorLogDirectory == null) {
            sErrorLogDirectory = new File(Constants.FILES_PATH, ERROR_DIRECTORY);
            FileManager.mkdir(sErrorLogDirectory.getAbsolutePath());
        }
        return sErrorLogDirectory;
    }

    /**
     * A general folder where unprocessed NDK crashes are saved.
     *
     * @return a folder name e.g. /lib/files/error/minidump/new
     */
    @NonNull
    public static synchronized File getNewMinidumpDirectory() {
        File errorStorageDirectory = getErrorStorageDirectory();
        File minidumpDirectory = new File(errorStorageDirectory.getAbsolutePath(), MINIDUMP_DIRECTORY);
        return new File(minidumpDirectory, NEW_MINIDUMP_DIRECTORY);
    }

    /**
     * A one-time run-specific folder where unprocessed NDK crashes are saved.
     *
     * @return a folder name e.g. /lib/files/error/minidump/new/aae16c29-42a9-baee-0777e6ba8fe3
     */
    @NonNull
    public static synchronized File getNewMinidumpSubfolder() {
        if (sNewMinidumpDirectory == null) {
            File minidumpDirectory = getNewMinidumpDirectory();
            sNewMinidumpDirectory = new File(minidumpDirectory, UUID.randomUUID().toString());
            FileManager.mkdir(sNewMinidumpDirectory.getPath());
        }
        return sNewMinidumpDirectory;
    }

    /**
     * A one-time run-specific folder where unprocessed NDK crashes are saved.
     * Each launch of the application creates its own sub-folder with a random name
     * to store information about the current device (including the application version),
     * which is used in the error report.
     *
     * @return a folder name e.g. /lib/files/error/minidump/new/aae16c29-f9e7-42a9-baee-0777e6ba8fe3
     */
    @NonNull
    public static synchronized File getNewMinidumpSubfolderWithContextData(Context context) {
        File directorySubfolder = getNewMinidumpSubfolder();
        File deviceInfoFile = new File(directorySubfolder, ErrorLogHelper.DEVICE_INFO_FILE);
        try {
            Device deviceInfo = DeviceInfoHelper.getDeviceInfo(context);
            String userIdContext = UserIdContext.getInstance().getUserId();
            String dataResidencyRegion = AppCenter.getDataResidencyRegion();
            deviceInfo.setWrapperSdkName(WRAPPER_SDK_NAME_NDK);

            /* To JSON. */
            JSONStringer writer = new JSONStringer();
            writer.object();
            deviceInfo.write(writer);
            writer.endObject();
            String deviceInfoString = writer.toString();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(DATA_RESIDENCY_REGION_KEY, dataResidencyRegion);
            jsonObject.put(DEVICE_INFO_KEY, deviceInfoString);
            jsonObject.put(USER_ID_KEY, userIdContext);

            /* Write file. */
            FileManager.write(deviceInfoFile, jsonObject.toString());
        } catch (DeviceInfoHelper.DeviceInfoException | IOException | JSONException e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to store device info in a minidump folder.", e);

            //noinspection ResultOfMethodCallIgnored
            deviceInfoFile.delete();
        }
        return directorySubfolder;
    }

    /**
     * A folder where minidumps of processed NDK crashes are saved.
     *
     * @return a folder name e.g. /lib/files/error/minidump/pending
     */
    @NonNull
    public static synchronized File getPendingMinidumpDirectory() {
        if (sPendingMinidumpDirectory == null) {
            File errorStorageDirectory = getErrorStorageDirectory();
            File minidumpDirectory = new File(errorStorageDirectory.getAbsolutePath(), MINIDUMP_DIRECTORY);
            sPendingMinidumpDirectory = new File(minidumpDirectory, PENDING_MINIDUMP_DIRECTORY);
            FileManager.mkdir(sPendingMinidumpDirectory.getPath());
        }
        return sPendingMinidumpDirectory;
    }

    @NonNull
    public static File[] getStoredErrorLogFiles() {
        File[] files = getErrorStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(ERROR_LOG_FILE_EXTENSION);
            }
        });
        return files != null ? files : new File[0];
    }

    @NonNull
    public static File[] getNewMinidumpFiles() {
        File[] files = getNewMinidumpDirectory().listFiles();
        return files != null ? files : new File[0];
    }

    /**
     * Get deviceInfo data.
     *
     * @param logFolder folder where to look for stored device information.
     * @return a device information or null.
     */
    @Nullable
    public static Device getStoredDeviceInfo(File logFolder) {
        String deviceInfoString = getContextInformation(logFolder);
        if (deviceInfoString == null) {
            return null;
        }
        return parseDevice(deviceInfoString);
    }

    /**
     * Get userId data.
     * @param logFolder folder where to look for stored userId.
     * @return userId or null.
     */
    public static String getStoredUserInfo(File logFolder) {
        String userInformationString = getContextInformation(logFolder);
        if (userInformationString == null) {
            return null;
        }
        return parseUserId(userInformationString);
    }

    /**
     * Get data residency region.
     *
     * @param logFolder folder where to look for stored data residency region.
     * @return a data residency region or null.
     */
    @Nullable
    public static String getStoredDataResidencyRegion(File logFolder) {
        String context = getContextInformation(logFolder);
        if (context == null) {
            return null;
        }
        return parseDataResidencyRegion(context);
    }

    /**
     * Get data about userId and deviceInfo in JSON format.
     * @param logFolder - path to folder where placed file with data about userId and deviceId.
     * @return - data about userId and deviceId in JSON format or null.
     */
    static String getContextInformation(File logFolder) {
        File[] files = logFolder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.equals(DEVICE_INFO_FILE);
            }
        });
        if (files == null || files.length == 0) {
            AppCenterLog.warn(Crashes.LOG_TAG, "No stored deviceinfo file found in a minidump folder.");
            return null;
        }
        File deviceInfoFile = files[0];
        String contextInfoString = FileManager.read(deviceInfoFile);
        if (contextInfoString == null) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to read stored device info.");
            return null;
        }
        return contextInfoString;
    }

    /**
     * Look for 'userId' data in file inside the minidump folder and parse it.
     * @param contextInformation - data with information about userId.
     * @return userId or null.
     */
    @VisibleForTesting
    static String parseUserId(String contextInformation) {
        try {
            JSONObject jsonObject = new JSONObject(contextInformation);
            if (jsonObject.has(USER_ID_KEY)) {
                return jsonObject.getString(USER_ID_KEY);
            }
        } catch (JSONException e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to deserialize user info.", e);
        }
        return null;
    }

    /**
     * Look for 'deviceInfo' data in file inside the minidump folder and parse it.
     * @param contextInformation - data with information about userId.
     * @return deviceInfo or null.
     */
    @VisibleForTesting
    static Device parseDevice(String contextInformation) {
        try {
            Device device = new Device();
            JSONObject jsonObject = new JSONObject(contextInformation);
            JSONObject deviceJson;
            if (jsonObject.has(DEVICE_INFO_KEY)) {
                deviceJson = new JSONObject(jsonObject.getString(DEVICE_INFO_KEY));
            } else {
                deviceJson = jsonObject;
            }
            device.read(deviceJson);
            return device;
        } catch (JSONException e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to deserialize device info.", e);
        }
        return null;
    }

    /**
     * Look for 'dataResidencyRegion' data in file inside the minidump folder and parse it.
     * @param contextInformation - data with information about userId.
     * @return dataResidencyRegion or null.
     */
    @VisibleForTesting
    static String parseDataResidencyRegion(String contextInformation) {
        try {
            JSONObject jsonObject = new JSONObject(contextInformation);
            if (jsonObject.has(DATA_RESIDENCY_REGION_KEY)) {
                return jsonObject.getString(DATA_RESIDENCY_REGION_KEY);
            }
        } catch (JSONException e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to deserialize data residency region.", e);
        }
        return null;
    }

    /**
     * Remove the minidump sub-folders from previous sessions in the 'minidump/new' folder.
     * Minidumps from these folders should already be moved to the 'minidump/pending' folder,
     * so that they can be safely deleted.
     */
    public static void removeStaleMinidumpSubfolders() {
        File[] previousSubFolders = getNewMinidumpDirectory().listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (sNewMinidumpDirectory != null) {
                    return !name.equals(sNewMinidumpDirectory.getName());
                }
                return true;
            }
        });
        if (previousSubFolders == null || previousSubFolders.length == 0) {
            AppCenterLog.debug(Crashes.LOG_TAG, "No previous minidump sub-folders.");
            return;
        }
        for (File file : previousSubFolders) {
            FileManager.deleteDirectory(file);
        }
    }

    /**
     * Remove the minidump folder.
     */
    public static void removeMinidumpFolder() {
        File errorStorageDirectory = getErrorStorageDirectory();
        File minidumpDirectory = new File(errorStorageDirectory.getAbsolutePath(), MINIDUMP_DIRECTORY);
        FileManager.deleteDirectory(minidumpDirectory);
    }

    @Nullable
    public static File getLastErrorLogFile() {
        return FileManager.lastModifiedFile(getErrorStorageDirectory(), new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(ERROR_LOG_FILE_EXTENSION);
            }
        });
    }

    @Nullable
    public static File getStoredThrowableFile(@NonNull UUID id) {
        return getStoredFile(id, THROWABLE_FILE_EXTENSION);
    }

    public static void removeStoredThrowableFile(@NonNull UUID id) {
        File file = getStoredThrowableFile(id);
        if (file != null) {
            AppCenterLog.info(Crashes.LOG_TAG, "Deleting throwable file " + file.getName());
            FileManager.delete(file);
        }
    }

    @Nullable
    static File getStoredErrorLogFile(@NonNull UUID id) {
        return getStoredFile(id, ERROR_LOG_FILE_EXTENSION);
    }

    public static void removeStoredErrorLogFile(@NonNull UUID id) {
        File file = getStoredErrorLogFile(id);
        if (file != null) {
            AppCenterLog.info(Crashes.LOG_TAG, "Deleting error log file " + file.getName());
            FileManager.delete(file);
        }
    }

    /**
     * Remove throwable files.
     */
    public static void removeLostThrowableFiles() {
        File[] throwableFiles = getErrorStorageDirectory().listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(THROWABLE_FILE_EXTENSION);
            }
        });
        if (throwableFiles != null && throwableFiles.length > 0) {
            for (File file : throwableFiles) {
                removeStoredThrowableFile(UUID.fromString(file.getName().replaceFirst("\\.[^.]+$", "")));
            }
        }
    }

    @NonNull
    public static ErrorReport getErrorReportFromErrorLog(@NonNull ManagedErrorLog log, String stackTrace) {
        ErrorReport report = new ErrorReport();
        report.setId(log.getId().toString());
        report.setThreadName(log.getErrorThreadName());
        report.setStackTrace(stackTrace);
        report.setAppStartTime(log.getAppLaunchTimestamp());
        report.setAppErrorTime(log.getTimestamp());
        report.setDevice(log.getDevice());
        return report;
    }

    public static void setErrorLogDirectory(File file) {
        sErrorLogDirectory = file;
    }

    @Nullable
    private static File getStoredFile(@NonNull final UUID id, @NonNull final String extension) {
        File[] files = getErrorStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(id.toString()) && filename.endsWith(extension);
            }
        });

        return files != null && files.length > 0 ? files[0] : null;
    }

    @NonNull
    public static Exception getModelExceptionFromThrowable(@NonNull Throwable t) {
        Exception topException = null;
        Exception parentException = null;
        List<Throwable> causeChain = new LinkedList<>();
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            causeChain.add(cause);
        }
        if (causeChain.size() > CAUSE_LIMIT) {
            AppCenterLog.warn(Crashes.LOG_TAG, "Crash causes truncated from " + causeChain.size() + " to " + CAUSE_LIMIT + " causes.");
            causeChain.subList(CAUSE_LIMIT_HALF, causeChain.size() - CAUSE_LIMIT_HALF).clear();
        }
        for (Throwable cause : causeChain) {
            Exception exception = new Exception();
            exception.setType(cause.getClass().getName());
            exception.setMessage(cause.getMessage());
            exception.setFrames(getModelFramesFromStackTrace(cause));
            if (topException == null) {
                topException = exception;
            } else {
                parentException.setInnerExceptions(Collections.singletonList(exception));
            }
            parentException = exception;
        }

        //noinspection ConstantConditions
        return topException;
    }

    @NonNull
    private static List<StackFrame> getModelFramesFromStackTrace(@NonNull Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > FRAME_LIMIT) {
            StackTraceElement[] stackTraceTruncated = new StackTraceElement[FRAME_LIMIT];
            System.arraycopy(stackTrace, 0, stackTraceTruncated, 0, FRAME_LIMIT_HALF);
            System.arraycopy(stackTrace, stackTrace.length - FRAME_LIMIT_HALF, stackTraceTruncated, FRAME_LIMIT_HALF, FRAME_LIMIT_HALF);
            throwable.setStackTrace(stackTraceTruncated);
            AppCenterLog.warn(Crashes.LOG_TAG, "Crash frames truncated from " + stackTrace.length + " to " + stackTraceTruncated.length + " frames.");
            stackTrace = stackTraceTruncated;
        }
        return getModelFramesFromStackTrace(stackTrace);
    }

    @NonNull
    private static List<StackFrame> getModelFramesFromStackTrace(@NonNull StackTraceElement[] stackTrace) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stackFrames.add(getModelStackFrame(stackTraceElement));
        }
        return stackFrames;
    }

    @NonNull
    private static StackFrame getModelStackFrame(StackTraceElement stackTraceElement) {
        StackFrame stackFrame = new StackFrame();
        stackFrame.setClassName(stackTraceElement.getClassName());
        stackFrame.setMethodName(stackTraceElement.getMethodName());
        stackFrame.setLineNumber(stackTraceElement.getLineNumber());
        stackFrame.setFileName(stackTraceElement.getFileName());
        return stackFrame;
    }

    /**
     * Validates properties.
     *
     * @param properties Properties collection to validate.
     * @param logType    Log type.
     * @return valid properties collection with maximum size of 5.
     */
    public static Map<String, String> validateProperties(Map<String, String> properties, String logType) {
        if (properties == null) {
            return null;
        }
        String message;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (result.size() >= MAX_PROPERTY_COUNT) {
                message = String.format("%s : properties cannot contain more than %s items. Skipping other properties.", logType, MAX_PROPERTY_COUNT);
                AppCenterLog.warn(Crashes.LOG_TAG, message);
                break;
            }
            if (key == null || key.isEmpty()) {
                message = String.format("%s : a property key cannot be null or empty. Property will be skipped.", logType);
                AppCenterLog.warn(Crashes.LOG_TAG, message);
                continue;
            }
            if (value == null) {
                message = String.format("%s : property '%s' : property value cannot be null. Property '%s' will be skipped.", logType, key, key);
                AppCenterLog.warn(Crashes.LOG_TAG, message);
                continue;
            }
            if (key.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s : property '%s' : property key length cannot be longer than %s characters. Property key will be truncated.", logType, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(Crashes.LOG_TAG, message);
                key = key.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            if (value.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s : property '%s' : property value cannot be longer than %s characters. Property value will be truncated.", logType, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(Crashes.LOG_TAG, message);
                value = value.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * Clear (delete all content) pending minidump directory.
     */
    public static void cleanPendingMinidumps() {
        FileManager.cleanDirectory(ErrorLogHelper.getPendingMinidumpDirectory());
    }

    /**
     * Parse log folder name UUID. Fallback to random UUID.
     *
     * @param logFolder a folder, e.g. lib/files/error/minidump/new/a80da2ae-8c85-43b0-a25b-d52319fb6d56
     * @return parsed UUID or random UUID.
     */
    @NonNull
    public static UUID parseLogFolderUuid(File logFolder) {
        UUID uuid = null;
        if (logFolder.isDirectory()) {
            try {
                uuid = UUID.fromString(logFolder.getName());
            } catch (IllegalArgumentException e) {
                AppCenterLog.warn(Crashes.LOG_TAG, "Cannot parse minidump folder name to UUID.", e);
            }
        }
        return uuid == null ? UUID.randomUUID() : uuid;
    }

    @VisibleForTesting
    public static void clearStaticState() {
        sNewMinidumpDirectory = null;
        sErrorLogDirectory = null;
        sPendingMinidumpDirectory = null;
    }
}
