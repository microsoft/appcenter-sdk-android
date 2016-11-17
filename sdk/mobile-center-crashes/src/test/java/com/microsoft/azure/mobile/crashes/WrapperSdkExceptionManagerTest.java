package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageHelper.InternalStorage.class, Crashes.class})

public class WrapperSdkExceptionManagerTest {
    @Before
    public void setUp() {
        mockStatic(StorageHelper.InternalStorage.class);
    }

    @Test
    public void fileIOExceptions() throws IOException, ClassNotFoundException, Exception {

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenThrow(new IOException()).thenThrow(new ClassNotFoundException());


        WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID().toString());
        WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID().toString());

        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.writeObject(any(File.class), any(String.class));
        WrapperSdkExceptionManager.saveWrapperExceptionData(new byte[]{}, UUID.randomUUID().toString());
    }
}
