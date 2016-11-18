package com.microsoft.azure.mobile.crashes;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@SuppressWarnings("WeakerAccess")
public class WrapperSdkExceptionManager {

    /**
     * File extension for data files created by this class.
     */
    private static final String DATA_FILE_EXTENSION = ".dat";

    /**
     * Contains wrapper SDK data that has been loaded into memory
     */
    private static final Map<String, byte[]> sWrapperExceptionDataContainer = new HashMap<>();

    @VisibleForTesting
    WrapperSdkExceptionManager() {
    }

    /**
     * Store the in-memory wrapper exception data to disk. This should only be used by a wrapper SDK.
     *
     * @param data  The data to be saved
     * @param errorId The associated error UUID
     */
    public static void saveWrapperExceptionData(byte[] data, UUID errorId) {
        if (errorId == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to save wrapper exception data: null errorId");
            return;
        }

        sWrapperExceptionDataContainer.put(errorId.toString(), data);
        File dataFile = getFile(errorId);
        try {
            StorageHelper.InternalStorage.writeObject(dataFile, data);
        }
        catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to save wrapper exception data to file", e);
        }
    }

    /**
     * Delete wrapper exception data from disk and store it in memory
     *
     * @param errorId    The associated error UUID
     */
    public static void deleteWrapperExceptionData(UUID errorId) {
        if (errorId == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: null errorId");
            return;
        }

        File dataFile = getFile(errorId);
        byte[] loadResult = loadWrapperExceptionData(errorId);
        if (loadResult == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Failed to delete wrapper exception data: data not found");
            return;
        }
        StorageHelper.InternalStorage.delete(dataFile);
    }

    /**
     * Load wrapper exception data into memory
     *
     * @param errorId   The associated error UUID
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
        }
        catch (ClassNotFoundException | IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot access wrapper exception data file " + dataFile.getName(), e);
        }
        return null;
    }

    /**
     * Get a file object for wrapper exception data
     *
     * @param errorId   The associated error UUID
     * @return The corresponding file object
     */
    private static File getFile(@NonNull UUID errorId) {
        File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
        String filename = errorId.toString() + DATA_FILE_EXTENSION;
        return new File(errorStorageDirectory, filename);
    }

    /**
     * Save error log modified by a wrapper SDK.
     *
     * @param errorLog error log to  save or overwrite.
     */
    @SuppressWarnings("WeakerAccess")
    public static void saveWrapperSdkErrorLog(ManagedErrorLog errorLog) {
        try {
            Crashes.getInstance().saveErrorLog(errorLog, ErrorLogHelper.getErrorStorageDirectory(), errorLog.getId().toString());
        } catch (JSONException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error serializing error log to JSON", e);
        } catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error writing error log to file", e);
        }
    }
}

