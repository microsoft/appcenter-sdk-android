/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.text.TextUtils;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

@SuppressWarnings("unused")
@PrepareForTest({FileManager.class, AppCenterLog.class, TextUtils.class})
public class FileManagerTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Test
    public void readFileNotFound() throws Exception {
        mockStatic(AppCenterLog.class);
        FileReader fileReader = mock(FileReader.class, new ThrowsException(new FileNotFoundException()));
        whenNew(FileReader.class).withAnyArguments().thenReturn(fileReader);
        assertNull(FileManager.read(new File("")));
        verify(fileReader).close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readError() throws Exception {
        mockStatic(AppCenterLog.class);
        BufferedReader reader = mock(BufferedReader.class);
        whenNew(BufferedReader.class).withAnyArguments().thenReturn(reader);
        whenNew(FileReader.class).withAnyArguments().thenReturn(mock(FileReader.class));
        when(reader.readLine()).thenReturn("incomplete");
        when(reader.readLine()).thenThrow(new EOFException());
        assertNull(FileManager.read(new File("")));
        verify(reader).close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readErrorAndCloseError() throws Exception {
        mockStatic(AppCenterLog.class);
        FileReader fileReader = mock(FileReader.class, new ThrowsException(new IOException()));
        whenNew(FileReader.class).withAnyArguments().thenReturn(fileReader);
        assertNull(FileManager.read(new File("")));
        verify(fileReader).close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readEmptyFile() throws Exception {
        mockStatic(AppCenterLog.class);
        BufferedReader reader = mock(BufferedReader.class);
        whenNew(BufferedReader.class).withAnyArguments().thenReturn(reader);
        whenNew(FileReader.class).withAnyArguments().thenReturn(mock(FileReader.class));
        when(reader.readLine()).thenReturn(null);
        assertEquals("", FileManager.read(new File("")));
        verify(reader).close();
    }

    @Test(expected = IOException.class)
    public void writeError() throws Exception {
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty(anyString())).thenReturn(false);
        when(TextUtils.getTrimmedLength(anyString())).thenReturn(4);
        BufferedWriter writer = mock(BufferedWriter.class);
        whenNew(BufferedWriter.class).withAnyArguments().thenReturn(writer);
        whenNew(FileWriter.class).withAnyArguments().thenReturn(mock(FileWriter.class));
        doThrow(new IOException("mock")).when(writer).write(anyString());
        FileManager.write(mock(File.class), "test");
        verify(writer).close();
    }

    @Test
    public void lastModifiedFile() {
        File dir = mock(File.class);
        FilenameFilter filter = mock(FilenameFilter.class);
        when(dir.exists()).thenReturn(true);
        when(dir.listFiles(any(FilenameFilter.class))).thenReturn(null);

        assertNull(FileManager.lastModifiedFile(dir, filter));

        File file1 = mock(File.class);
        File file2 = mock(File.class);
        File file3 = mock(File.class);
        when(file1.lastModified()).thenReturn(1L);
        when(file2.lastModified()).thenReturn(2L);
        when(file3.lastModified()).thenReturn(3L);
        when(dir.listFiles(filter)).thenReturn(new File[]{file1, file3, file2});

        assertEquals(file3, FileManager.lastModifiedFile(dir, filter));
    }

    @Test
    public void readBytesError() throws Exception {
        mockStatic(AppCenterLog.class);
        FileInputStream fileInputStream = mock(FileInputStream.class);
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(fileInputStream);
        DataInputStream dataInputStream = mock(DataInputStream.class);
        whenNew(DataInputStream.class).withAnyArguments().thenReturn(dataInputStream);
        doThrow(new IOException("mock")).when(dataInputStream).readFully(any(byte[].class));
        assertNull(FileManager.readBytes(new File("")));
        verify(fileInputStream).close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readBytesErrorAndCloseError() throws Exception {
        mockStatic(AppCenterLog.class);
        FileInputStream fileInputStream = mock(FileInputStream.class);
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(fileInputStream);
        doThrow(new IOException("mock close")).when(fileInputStream).close();
        DataInputStream dataInputStream = mock(DataInputStream.class);
        whenNew(DataInputStream.class).withAnyArguments().thenReturn(dataInputStream);
        doThrow(new IOException("mock")).when(dataInputStream).readFully(any(byte[].class));
        assertNull(FileManager.readBytes(new File("")));
        verify(fileInputStream).close();
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void deleteFilesWhenFileListIsNull() throws IOException {
        spy(FileManager.class);

        /* Prepare data. */
        File folder = mTemporaryFolder.newFolder();
        assertTrue(folder.exists());

        /* Remove directory. */
        FileManager.deleteDirectory(folder);

        /* Verify. */
        assertFalse(folder.exists());
        verifyStatic(FileManager.class);
        FileManager.deleteDirectory(any(File.class));
    }

    @Test
    public void deleteFiles() throws IOException {
        spy(FileManager.class);

        /* Prepare data. */
        File folder = mTemporaryFolder.newFolder();
        File subfolder = new File(folder, "subfolder");
        assertTrue(subfolder.mkdir());
        assertTrue(subfolder.exists());
        File file1 = new File(subfolder, "file1");
        assertTrue(file1.createNewFile());
        assertTrue(file1.exists());
        File file2 = new File(subfolder, "file2");
        assertTrue(file2.createNewFile());
        assertTrue(file2.exists());

        /* Remove directory. */
        FileManager.deleteDirectory(folder);

        assertFalse(file2.exists());
        assertFalse(file1.exists());
        assertFalse(subfolder.exists());
        assertFalse(folder.exists());

        /* Verify. */
        verifyStatic(FileManager.class, times(4));
        FileManager.deleteDirectory(any(File.class));
    }

    @Test
    public void cleanDirectory() throws IOException {

        /* Prepare data. */
        File folder = mTemporaryFolder.newFolder();
        File file1 = new File(folder, "file1");
        assertTrue(file1.createNewFile());
        assertTrue(file1.exists());
        File file2 = new File(folder, "file2");
        assertTrue(file2.createNewFile());
        assertTrue(file2.exists());

        /* Clean directory. */
        FileManager.cleanDirectory(folder);

        /* Verify. */
        assertFalse(file2.exists());
        assertFalse(file1.exists());
        assertTrue(folder.exists());
    }

    @Test
    public void cleanDirectoryWhenNotDirectory() throws IOException {

        /* Prepare data. */
        File folder = mTemporaryFolder.newFolder();
        File file1 = new File(folder, "file1");
        assertTrue(file1.createNewFile());
        assertTrue(file1.exists());

        /* Verify when directory is not directory, that no crashes happen */
        FileManager.cleanDirectory(file1);
    }

    @Test
    public void getNameWithDotAtTheEnd() {
        File mockFile = mock(File.class);
        String fileName = "someName.";
        when(mockFile.getName()).thenReturn(fileName);
        String result = FileManager.getNameWithoutExtension(mockFile);
        assertEquals(result, fileName);
    }

    @Test
    public void getNameWithDotAtTheStart() {
        File mockFile = mock(File.class);
        String fileName = ".someName";
        when(mockFile.getName()).thenReturn(fileName);
        String result = FileManager.getNameWithoutExtension(mockFile);
        assertEquals(result, fileName);
    }

    @Test
    public void getNameWithOutExtension() {
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("someName.ext");
        String result = FileManager.getNameWithoutExtension(mockFile);
        assertEquals(result, "someName");
    }

    @Test
    public void getNameWithDotsWithOutExtension() {
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("someName.someInfo.ext");
        String result = FileManager.getNameWithoutExtension(mockFile);
        assertEquals(result, "someName.someInfo");
    }
}
