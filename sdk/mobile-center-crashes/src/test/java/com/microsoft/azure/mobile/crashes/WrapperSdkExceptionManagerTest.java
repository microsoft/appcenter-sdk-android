package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import static junit.framework.Assert.assertNull;
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
        PowerMockito.doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID()));
        PowerMockito.doThrow(new ClassNotFoundException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID()));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(null));
    }

    @Test
    public void deleteWrapperExceptionDataWithNullId() throws Exception {
        WrapperSdkExceptionManager.deleteWrapperExceptionData(null);
        verifyStatic(Mockito.never());
        StorageHelper.InternalStorage.delete(any(File.class));

        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic(Mockito.never());
        StorageHelper.InternalStorage.delete(any(File.class));
    }

    @Test
    public void saveWrapperExceptionData() throws IOException {
        // cannot save with a null uuid
        WrapperSdkExceptionManager.saveWrapperExceptionData(null, null);
        verifyStatic(Mockito.never());
        StorageHelper.InternalStorage.writeObject(any(File.class), anyString());

        // ok to save null if there is a uuid
        WrapperSdkExceptionManager.saveWrapperExceptionData(null, UUID.randomUUID()); //save null data
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), any(Serializable.class));
        
        PowerMockito.doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.writeObject(any(File.class), anyString());
        WrapperSdkExceptionManager.saveWrapperExceptionData(null, UUIDUtils.randomUUID());
        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), any(IOException.class));
    }
}
