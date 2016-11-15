package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.io.File;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class WrapperSdkExceptionManager {
    private static byte[] mManagedExceptionData = null;
    private static Map<String, byte[]> mManagedExceptionDataContainer = new HashMap<>();

    public static boolean hasManagedExceptionData() {
        return mManagedExceptionData != null;
    }

    public static byte[] getManagedExceptionData(String reportIdString) {
        return loadManagedExceptionData(reportIdString);
    }

    public static void setManagedExceptionData(byte[] data) {
        mManagedExceptionData = data;
    }

    public static void saveManagedExceptionData(UUID reportId) {
        mManagedExceptionDataContainer.put(reportId.toString(), mManagedExceptionData);
        File dataFile = getFile(reportId);
        try {
            StorageHelper.InternalStorage.writeObject(dataFile, mManagedExceptionData);
        }
        catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error writing wrapper exception data to file", e);
        }
    }

    public static void deleteManagedExceptionData(UUID reportId) {
        File dataFile = getFile(reportId);
        loadManagedExceptionData(reportId); //save the exception data in memory before deleting
        StorageHelper.InternalStorage.delete(dataFile);
    }


    private static byte[] loadManagedExceptionData(UUID reportId) {
        return loadManagedExceptionData(reportId.toString());
    }

    private static byte[] loadManagedExceptionData(String reportIdString) {
        File dataFile = getFile(reportIdString);
        try {
            byte[] dataBytes = StorageHelper.InternalStorage.readObject(dataFile);
            if (dataBytes != null) {
                mManagedExceptionDataContainer.put(reportIdString, dataBytes);
            }
            return dataBytes;
        }
        catch (ClassNotFoundException ignored) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot read throwable file " + dataFile.getName(), ignored);
        }
        catch (IOException ignored) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Cannot access serialized throwable file " + dataFile.getName(), ignored);
        }

        return null;
    }
    private static File getFile(UUID reportId) {
        return getFile(reportId.toString());
    }
    private static File getFile(String reportIdString) {
        File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
        String filename = reportIdString + ".dat";
        return new File(errorStorageDirectory, filename);
    }
}

