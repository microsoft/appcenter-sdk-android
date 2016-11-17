package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;

import java.io.File;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;


public class WrapperSdkExceptionManager {
    /**
     * File extension for data files created by this class.
     */
    private static final String DATA_FILE_EXTENSION = ".dat";

    /**
     * Contains wrapper SDK data that has been loaded into memory
     */
    private static Map<String, byte[]> sWrapperExceptionDataContainer = new HashMap<>();

    /**
     * Store the in-memory wrapper exception data to disk. This should only be used by a wrapper SDK.
     *
     * @param data  The data to be saved
     * @param reportIdString  The associated error UUID string
     */
    public static void saveWrapperExceptionData(byte[] data, String reportIdString) {
        if (reportIdString == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Trying to save wrapper exception data with null UUID");
            return;
        }

        sWrapperExceptionDataContainer.put(reportIdString.toString(), data);
        File dataFile = getFile(reportIdString.toString());
        try {
            StorageHelper.InternalStorage.writeObject(dataFile, data);
        }
        catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error writing wrapper exception data to file", e);
        }
    }

    /**
     * Delete wrapper exception data from disk and store it in memory
     *
     * @param reportId    The associated error UUID
     */
    public static void deleteWrapperExceptionData(UUID reportId) {
        if (reportId == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Trying to delete wrapper exception data with null UUID");
            return;
        }

        File dataFile = getFile(reportId.toString());
        byte[] loadResult = loadWrapperExceptionData(reportId.toString());
        if (loadResult == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Trying to delete wrapper exception data with null UUID");
            return;
        }
        StorageHelper.InternalStorage.delete(dataFile);
    }

    /**
     * Load wrapper exception data into memory
     *
     * @param reportIdString   String representation of the associated error UUID
     * @return The data loaded into memory
     */
    public static byte[] loadWrapperExceptionData(String reportIdString) {
        if (reportIdString == null) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Trying to load wrapper exception data with null UUID");
            return null;
        }

        byte[] dataBytes = sWrapperExceptionDataContainer.get(reportIdString);
        if (dataBytes != null) {
            return dataBytes;
        }

        File dataFile = getFile(reportIdString);

        try {
            dataBytes = StorageHelper.InternalStorage.readObject(dataFile);
            if (dataBytes != null) {
                sWrapperExceptionDataContainer.put(reportIdString, dataBytes);
            }
            return dataBytes;
        }
        catch (ClassNotFoundException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot read wrapper exception data file " + dataFile.getName(), e);
        }
        catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot access wrapper exception data file " + dataFile.getName(), e);
        }
        return null;
    }

    /**
     * Get a file object for wrapper exception data
     *
     * @param reportIdString   String representation of the associated error UUID
     * @return The corresponding file object
     */
    private static File getFile(String reportIdString) {
        File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
        String filename = reportIdString + DATA_FILE_EXTENSION;
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

