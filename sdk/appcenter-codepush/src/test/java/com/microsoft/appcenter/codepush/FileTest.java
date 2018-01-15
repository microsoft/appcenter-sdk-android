package com.microsoft.appcenter.codepush;

import android.os.Environment;

import com.microsoft.appcenter.codepush.utils.FileUtils;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class FileTest {

    @Test
    public void fileTest() throws Exception {

        /* Creating files and directories. */
        String testString = "123";
        String newName = "newFileName.txt";
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "/Test");
        File testDirMove = new File(Environment.getExternalStorageDirectory(), "/TestMove");
        testDir.mkdir();
        testDirMove.mkdir();
        File newFile = new File(testDir, fileName);
        newFile.createNewFile();
        assertEquals(true, FileUtils.fileAtPathExists(newFile.getPath()));

        /* Testing write/read. */
        FileUtils.writeStringToFile(testString, newFile.getPath());
        assertEquals(testString, FileUtils.readFileToString(newFile.getPath()));

        /* Testing move/copy. */
        FileUtils.moveFile(newFile, testDirMove.getPath(), newName);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirMove.getPath(), newName)));
        assertEquals(false, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDir.getPath(), fileName)));
        FileUtils.copyDirectoryContents(testDirMove.getPath(), testDir.getPath());
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirMove.getPath(), newName)));
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDir.getPath(), newName)));

        /* Testing delete. */
        FileUtils.deleteDirectoryAtPath(testDirMove.getPath());
        assertEquals(false, FileUtils.fileAtPathExists(testDirMove.getPath()));
    }

    @Test
    public void zipTest() throws Exception {
        String testString = "123";
        String zipFileName = "test.zip";
        String zipEntryFileName = "mytext.txt";
        File testDirZip = new File(Environment.getExternalStorageDirectory(), "/TestZip");
        testDirZip.mkdir();
        File testDirZipMove = new File(Environment.getExternalStorageDirectory(), "/TestZipMove");
        testDirZipMove.mkdir();
        File zip = new File(testDirZip, zipFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        ZipEntry e = new ZipEntry(zipEntryFileName);
        out.putNextEntry(e);
        byte[] data = testString.getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        FileUtils.unzipFile(zip, testDirZipMove.getPath());
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirZipMove.getPath(), zipEntryFileName)));
    }
}
