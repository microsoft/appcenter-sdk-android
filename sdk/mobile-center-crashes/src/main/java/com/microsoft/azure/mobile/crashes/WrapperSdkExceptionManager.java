package com.microsoft.azure.mobile.crashes;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.io.File;
import java.io.IOException;
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
     * @param modelException         model exception.
     * @param rawSerializedException raw exception bytes.
     * @return error log identifier if successful or null if failed to save to disk.
     */
    public static UUID saveWrapperException(Thread thread, com.microsoft.azure.mobile.crashes.ingestion.models.Exception modelException, byte[] rawSerializedException) {
        try {
            UUID errorId = Crashes.getInstance().saveUncaughtException(thread, null, modelException);
            if (errorId != null) {
                sWrapperExceptionDataContainer.put(errorId.toString(), rawSerializedException);
                File dataFile = getFile(errorId);
                StorageHelper.InternalStorage.writeObject(dataFile, rawSerializedException);
                MobileCenterLog.debug(Crashes.LOG_TAG, "Saved raw wrapper exception data into " + dataFile);
            }
            return errorId;
        } catch (Exception e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to save wrapper exception data to file", e);
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
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: null errorId");
            return;
        }
        File dataFile = getFile(errorId);
        if (dataFile.exists()) {
            byte[] loadResult = loadWrapperExceptionData(errorId);
            if (loadResult == null) {
                MobileCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: data not found");
            }
            StorageHelper.InternalStorage.delete(dataFile);
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
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to load wrapper exception data: null errorId");
            return null;
        }

        byte[] dataBytes = sWrapperExceptionDataContainer.get(errorId.toString());
        if (dataBytes != null) {
            return dataBytes;
        }

        File dataFile = getFile(errorId);

        try {
            dataBytes = StorageHelper.InternalStorage.readObject(dataFile);
            if (dataBytes != null) {
                sWrapperExceptionDataContainer.put(errorId.toString(), dataBytes);
            }
            return dataBytes;
        } catch (ClassNotFoundException | IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot access wrapper exception data file " + dataFile.getName(), e);
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
}

