package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCenterLog.class, StorageHelper.InternalStorage.class, Crashes.class})
public class WrapperSdkExceptionManagerTest {
    @Before
    public void setUp() {
        mockStatic(StorageHelper.InternalStorage.class);
    }

    @Test
    public void constructWrapperSdkExceptionManager() {
        new WrapperSdkExceptionManager();
    }

    @Test
    public void loadWrapperExceptionData() throws Exception {
        mockStatic(StorageHelper.InternalStorage.class);

        PowerMockito.doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID());
        PowerMockito.doThrow(new ClassNotFoundException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID());
    }

    @Test
    public void saveWrapperExceptionData() throws IOException {
        PowerMockito.doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.writeObject(any(File.class), anyString());
        WrapperSdkExceptionManager.saveWrapperExceptionData(null, UUIDUtils.randomUUID());
        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), any(IOException.class));
    }
}
