package com.microsoft.appcenter.codepush.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    /**
     * Size of the buffer used when writing files.
     */
    private static final int WRITE_BUFFER_SIZE = 1024 * 8;

    /**
     * Appends file path with one more component.
     *
     * @param basePath            path to be appended.
     * @param appendPathComponent path component to append the base path.
     * @return new path.
     */
    public static String appendPathComponent(String basePath, String appendPathComponent) {
        return new File(basePath, appendPathComponent).getAbsolutePath();
    }

    /**
     * Copies the contents of one directory to another. Copies all teh contents recursively.
     *
     * @param sourceDirectoryPath      path to the directory to copy files from.
     * @param destinationDirectoryPath path to the directory to copy files to.
     * @throws IOException exception occurred during read/write operations.
     */
    public static void copyDirectoryContents(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        File sourceDir = new File(sourceDirectoryPath);
        File destDir = new File(destinationDirectoryPath);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        for (File sourceFile : sourceDir.listFiles()) {
            if (sourceFile.isDirectory()) {
                copyDirectoryContents(
                        appendPathComponent(sourceDirectoryPath, sourceFile.getName()),
                        appendPathComponent(destinationDirectoryPath, sourceFile.getName()));
            } else {
                File destFile = new File(destDir, sourceFile.getName());
                FileInputStream fromFileStream = null;
                BufferedInputStream fromBufferedStream = null;
                FileOutputStream destStream = null;
                byte[] buffer = new byte[WRITE_BUFFER_SIZE];
                try {
                    fromFileStream = new FileInputStream(sourceFile);
                    fromBufferedStream = new BufferedInputStream(fromFileStream);
                    destStream = new FileOutputStream(destFile);
                    int bytesRead;
                    while ((bytesRead = fromBufferedStream.read(buffer)) > 0) {
                        destStream.write(buffer, 0, bytesRead);
                    }
                } finally {
                    try {
                        if (fromFileStream != null) fromFileStream.close();
                        if (fromBufferedStream != null) fromBufferedStream.close();
                        if (destStream != null) destStream.close();
                    } catch (IOException e) {
                      //  throw new CodePushUnknownException("Error closing IO resources.", e);
                    }
                }
            }
        }
    }

    /**
     * Deletes directory by the following path.
     *
     * @param directoryPath path to directory to be deleted. Can't be null.
     */
    public static void deleteDirectoryAtPath(String directoryPath) {
        if (directoryPath == null) {
           // CodePushUtils.log("deleteDirectoryAtPath attempted with null directoryPath");
            return;
        }
        File file = new File(directoryPath);
        if (file.exists()) {
            deleteFileOrFolderSilently(file);
        }
    }

    /**
     * Deletes file or folder throwing no errors if something went wrong.
     *
     * @param file path to file/folder to be deleted.
     */
    public static void deleteFileOrFolderSilently(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileEntry : files) {
                if (fileEntry.isDirectory()) {
                    deleteFileOrFolderSilently(fileEntry);
                } else {
                    fileEntry.delete();
                }
            }
        }

        if (!file.delete()) {
            //CodePushUtils.log("Error deleting file " + file.getName());
        }
    }

    /**
     * Checks whether a file by the following path exists.
     *
     * @param filePath path to be checked.
     * @return <code>true</code> if exists, <code>false</code> if not.
     */
    public static boolean fileAtPathExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * Moves file from one folder to another.
     *
     * @param fileToMove    path to the file to be moved.
     * @param newFolderPath path to be moved to.
     * @param newFileName   new name for the file to be moved.
     */
    public static void moveFile(File fileToMove, String newFolderPath, String newFileName) {
        File newFolder = new File(newFolderPath);
        if (!newFolder.exists()) {
            newFolder.mkdirs();
        }

        File newFilePath = new File(newFolderPath, newFileName);
        if (!fileToMove.renameTo(newFilePath)) {
          //  throw new CodePushUnknownException("Unable to move file from " +
              //      fileToMove.getAbsolutePath() + " to " + newFilePath.getAbsolutePath() + ".");
        }
    }

    /**
     * Reads the contents of file.
     *
     * @param filePath path to file to be read.
     * @return string with contents of the file.
     * @throws IOException exception occurred during read/write operations.
     */
    public static String readFileToString(String filePath) throws IOException {
        FileInputStream fin = null;
        BufferedReader reader = null;
        try {
            File fl = new File(filePath);
            fin = new FileInputStream(fl);
            reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString().trim();
        } finally {
            if (reader != null) reader.close();
            if (fin != null) fin.close();
        }
    }

    /**
     * Gets files from archive.
     *
     * @param zipFile     path to zip-archive.
     * @param destination path for the unzipped files to be saved.
     * @throws IOException exception occurred during read/write operations.
     */
    public static void unzipFile(File zipFile, String destination) throws IOException {
        FileInputStream fileStream = null;
        BufferedInputStream bufferedStream = null;
        ZipInputStream zipStream = null;
        try {
            fileStream = new FileInputStream(zipFile);
            bufferedStream = new BufferedInputStream(fileStream);
            zipStream = new ZipInputStream(bufferedStream);
            ZipEntry entry;

            File destinationFolder = new File(destination);
            if (destinationFolder.exists()) {
                deleteFileOrFolderSilently(destinationFolder);
            }

            destinationFolder.mkdirs();

            byte[] buffer = new byte[WRITE_BUFFER_SIZE];
            while ((entry = zipStream.getNextEntry()) != null) {
                String fileName = entry.getName();
                File file = new File(destinationFolder, fileName);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        int numBytesRead;
                        while ((numBytesRead = zipStream.read(buffer)) != -1) {
                            fout.write(buffer, 0, numBytesRead);
                        }
                    } finally {
                        fout.close();
                    }
                }
                long time = entry.getTime();
                if (time > 0) {
                    file.setLastModified(time);
                }
            }
        } finally {
            try {
                if (zipStream != null) zipStream.close();
                if (bufferedStream != null) bufferedStream.close();
                if (fileStream != null) fileStream.close();
            } catch (IOException e) {
               // throw new CodePushUnknownException("Error closing IO resources.", e);
            }
        }
    }

    /**
     * Writes some content to a file.
     *
     * @param content  content to be written to a file.
     * @param filePath path to a file.
     * @throws IOException exception occurred during read/write operations.
     */
    public static void writeStringToFile(String content, String filePath) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(filePath);
            out.print(content);
        } finally {
            if (out != null) out.close();
        }
    }
}
