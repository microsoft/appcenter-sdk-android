/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@SuppressWarnings("WeakerAccess")
public class WrapperSdkExceptionManager {

    /**
     * Contains wrapper SDK data that has been loaded into memory
     */
    @VisibleForTesting
    static final Map<String, byte[]> sWrapperExceptionDataContainer = new HashMap<>();

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
     * @param rawSerializedException raw exception bytes if available, can be null.
     * @return error log identifier if successful or null if failed to save to disk.
     */
    public static UUID saveWrapperException(Thread thread, Throwable throwable, com.microsoft.appcenter.crashes.ingestion.models.Exception modelException, byte[] rawSerializedException) {
        try {
            UUID errorId = Crashes.getInstance().saveUncaughtException(thread, throwable, modelException);
            if (errorId != null && rawSerializedException != null) {
                sWrapperExceptionDataContainer.put(errorId.toString(), rawSerializedException);
                File dataFile = getFile(errorId);
                FileManager.writeObject(dataFile, rawSerializedException);
                AppCenterLog.debug(Crashes.LOG_TAG, "Saved raw wrapper exception data into " + dataFile);
            }
            return errorId;
        } catch (Exception e) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to save wrapper exception data to file", e);
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
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: null errorId");
            return;
        }
        File dataFile = getFile(errorId);
        if (dataFile.exists()) {
            byte[] loadResult = loadWrapperExceptionData(errorId);
            if (loadResult == null) {
                AppCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: data not found");
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
    public static byte[] loadWrapperExceptionData(UUID errorId) {
        if (errorId == null) {
            AppCenterLog.error(Crashes.LOG_TAG, "Failed to load wrapper exception data: null errorId");
            return null;
        }
        byte[] dataBytes = sWrapperExceptionDataContainer.get(errorId.toString());
        if (dataBytes != null) {
            return dataBytes;
        }
        File dataFile = getFile(errorId);
        if (dataFile.exists()) {
            try {
                dataBytes = FileManager.readObject(dataFile);
                if (dataBytes != null) {
                    sWrapperExceptionDataContainer.put(errorId.toString(), dataBytes);
                }
                return dataBytes;
            } catch (ClassNotFoundException | IOException e) {
                AppCenterLog.error(Crashes.LOG_TAG, "Cannot access wrapper exception data file " + dataFile.getName(), e);
            }
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
     */
    public static void trackException(com.microsoft.appcenter.crashes.ingestion.models.Exception modelException) {
        trackException(modelException, null);
    }

    /**
     * Send an handled exception (used by wrapper SDKs).
     *
     * @param modelException An handled exception already in JSON model form.
     * @param properties     optional properties.
     */
    public static void trackException(com.microsoft.appcenter.crashes.ingestion.models.Exception modelException, Map<String, String> properties) {
        Map<String, String> validatedProperties = ErrorLogHelper.validateProperties(properties, "HandledError");
        Crashes.getInstance().queueException(modelException, validatedProperties);
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
     * Send error attachments when automatic processing is disabled.
     *
     * @param errorReportId The crash report identifier for additional information.
     * @param attachments   instances of {@link ErrorAttachmentLog} to be sent for the specified error report.
     */
    public static void sendErrorAttachments(String errorReportId, Iterable<ErrorAttachmentLog> attachments) {
        Crashes.getInstance().sendErrorAttachments(errorReportId, attachments);
    }
}

