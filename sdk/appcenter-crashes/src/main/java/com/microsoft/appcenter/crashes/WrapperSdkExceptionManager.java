/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.crashes.Crashes.LOG_TAG;

@SuppressWarnings("WeakerAccess")
public class WrapperSdkExceptionManager {

    /**
     * Contains wrapper SDK data that has been loaded into memory
     */
    @VisibleForTesting
    static final Map<String, String> sWrapperExceptionDataContainer = new HashMap<>();

    /**
     * File extension for data files created by this class.
     */
    private static final String DATA_FILE_EXTENSION = ".dat";

    @VisibleForTesting
    WrapperSdkExceptionManager() {
    }

    /**
     * Save a crash from wrapper SDK.
     *
     * @param thread                 thread where uncaught exception originated.
     * @param throwable              Java throwable for client side inspection if available, can be null.
     * @param modelException         model exception.
     * @param rawSerializedException raw exception string if available, can be null.
     * @return error log identifier if successful or null if failed to save to disk.
     */
    public static UUID saveWrapperException(Thread thread, Throwable throwable, com.microsoft.appcenter.crashes.ingestion.models.Exception modelException, String rawSerializedException) {
        try {
            UUID errorId = Crashes.getInstance().saveUncaughtException(thread, throwable, modelException);
            if (errorId != null && rawSerializedException != null) {
                sWrapperExceptionDataContainer.put(errorId.toString(), rawSerializedException);
                File dataFile = getFile(errorId);
                FileManager.write(dataFile, rawSerializedException);
                AppCenterLog.debug(LOG_TAG, "Saved raw wrapper exception data into " + dataFile);
            }
            return errorId;
        } catch (Exception e) {
            AppCenterLog.error(LOG_TAG, "Failed to save wrapper exception data to file", e);
            return null;
        }
    }

    /**
     * Delete wrapper exception data from disk and store it in memory
     *
     * @param errorId The associated error UUID
     */
    public static void deleteWrapperExceptionData(UUID errorId) {
        if (errorId == null) {
            AppCenterLog.error(LOG_TAG, "Failed to delete wrapper exception data: null errorId");
            return;
        }
        File dataFile = getFile(errorId);
        if (dataFile.exists()) {
            String loadResult = loadWrapperExceptionData(errorId);
            if (loadResult == null) {
                AppCenterLog.error(LOG_TAG, "Failed to load wrapper exception data.");
            }
            FileManager.delete(dataFile);
        }
    }

    /**
     * Load wrapper exception data into memory
     *
     * @param errorId The associated error UUID
     * @return The data loaded into memory
     */
    public static String loadWrapperExceptionData(UUID errorId) {
        if (errorId == null) {
            AppCenterLog.error(LOG_TAG, "Failed to load wrapper exception data: null errorId");
            return null;
        }
        String data = sWrapperExceptionDataContainer.get(errorId.toString());
        if (data != null) {
            return data;
        }
        File dataFile = getFile(errorId);
        if (dataFile.exists()) {
            data = FileManager.read(dataFile);
            if (data != null) {
                sWrapperExceptionDataContainer.put(errorId.toString(), data);
            }
            return data;
        }
        return null;
    }

    /**
     * Get a file object for wrapper exception data
     *
     * @param errorId The associated error UUID
     * @return The corresponding file object
     */
    private static File getFile(@NonNull UUID errorId) {
        File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
        String filename = errorId.toString() + DATA_FILE_EXTENSION;
        return new File(errorStorageDirectory, filename);
    }

    /**
     * Send an handled exception (used by wrapper SDKs).
     *
     * @param modelException An handled exception already in JSON model form.
     * @param properties     optional properties.
     * @param attachments    optional attachments.
     * @return error report ID.
     */
    public static String trackException(com.microsoft.appcenter.crashes.ingestion.models.Exception modelException, Map<String, String> properties, Iterable<ErrorAttachmentLog> attachments) {
        return Crashes.getInstance().queueException(modelException, properties, attachments).toString();
    }

    /**
     * Set whether automatic processing is enabled or not.
     * Default is enabled.
     *
     * @param automaticProcessing true to enable, false otherwise.
     */
    public static void setAutomaticProcessing(@SuppressWarnings("SameParameterValue") boolean automaticProcessing) {
        Crashes.getInstance().setAutomaticProcessing(automaticProcessing);
    }

    /**
     * Get unprocessed error reports when automatic processing is disabled.
     *
     * @return unprocessed error reports as an async future.
     */
    public static AppCenterFuture<Collection<ErrorReport>> getUnprocessedErrorReports() {
        return Crashes.getInstance().getUnprocessedErrorReports();
    }

    /**
     * Resume processing of crash reports with the filtered list from {@link #getUnprocessedErrorReports()}.
     * To use when automatic processing is disabled.
     *
     * @param filteredReportIds report identifiers to process, every crash not part of the original list are discarded.
     * @return asynchronous result: true if ALWAYS_SEND was previously set, false otherwise.
     */
    public static AppCenterFuture<Boolean> sendCrashReportsOrAwaitUserConfirmation(Collection<String> filteredReportIds) {
        return Crashes.getInstance().sendCrashReportsOrAwaitUserConfirmation(filteredReportIds);
    }

    /**
     * Get a generic error report representation for an handled exception.
     * Since this is used by wrapper SDKs, stack trace and thread name are not known at Java level.
     *
     * @param context       context.
     * @param errorReportId The error report identifier.
     * @return an error report.
     */
    public static ErrorReport buildHandledErrorReport(Context context, String errorReportId) {
        ErrorReport report = new ErrorReport();
        report.setId(errorReportId);
        report.setAppErrorTime(new Date());
        report.setAppStartTime(new Date(Crashes.getInstance().getInitializeTimestamp()));
        try {
            report.setDevice(Crashes.getInstance().getDeviceInfo(context));
        } catch (DeviceInfoHelper.DeviceInfoException e) {

            /* The exception is already logged, just adding this log for context and avoid empty catch. */
            AppCenterLog.warn(LOG_TAG, "Handled error report cannot get device info, errorReportId=" + errorReportId);
        }
        return report;
    }

    /**
     * Send error attachments when automatic processing is disabled.
     *
     * @param errorReportId The error report identifier to send attachments for.
     * @param attachments   instances of {@link ErrorAttachmentLog} to be sent for the specified error report.
     */
    public static void sendErrorAttachments(String errorReportId, Iterable<ErrorAttachmentLog> attachments) {
        Crashes.getInstance().sendErrorAttachments(errorReportId, attachments);
    }
}

