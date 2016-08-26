package com.microsoft.sonoma.core;

import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({StorageHelper.InternalStorage.class, FileReader.class, BufferedReader.class, SonomaLog.class})
public class StorageHelperTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void readFileNotFound() throws Exception {
        mockStatic(SonomaLog.class);
        FileReader fileReader = mock(FileReader.class, new ThrowsException(new FileNotFoundException()));
        whenNew(FileReader.class).withAnyArguments().thenReturn(fileReader);
        assertNull(StorageHelper.InternalStorage.read(new File("")));
        verify(fileReader).close();
        verifyStatic();
        SonomaLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readError() throws Exception {
        mockStatic(SonomaLog.class);
        BufferedReader reader = mock(BufferedReader.class);
        whenNew(BufferedReader.class).withAnyArguments().thenReturn(reader);
        whenNew(FileReader.class).withAnyArguments().thenReturn(mock(FileReader.class));
        when(reader.readLine()).thenReturn("incomplete");
        when(reader.readLine()).thenThrow(new EOFException());
        assertNull(StorageHelper.InternalStorage.read(new File("")));
        verify(reader).close();
        verifyStatic();
        SonomaLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void readErrorAndCloseError() throws Exception {
        mockStatic(SonomaLog.class);
        FileReader fileReader = mock(FileReader.class, new ThrowsException(new IOException()));
        whenNew(FileReader.class).withAnyArguments().thenReturn(fileReader);
        assertNull(StorageHelper.InternalStorage.read(new File("")));
        verify(fileReader).close();
        verifyStatic();
        SonomaLog.error(anyString(), anyString(), any(IOException.class));
    }
}
