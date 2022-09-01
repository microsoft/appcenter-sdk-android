package com.microsoft.appcenter.utils;

import java.io.File;

public class FileHelper {

    public static String getNameWithOutExtension(File file) {
        String fileName = file.getName();
        int indexOfLastDot = fileName.lastIndexOf(".");
        if (indexOfLastDot > 0 && indexOfLastDot < fileName.length() - 1) {
            return fileName.substring(0, indexOfLastDot);
        }
        return fileName;
    }

}
