package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({WrapperSdkExceptionManager.class, MobileCenterLog.class, StorageHelper.InternalStorage.class, Crashes.class, ErrorLogHelper.class})
public class WrapperSdkExceptionManagerTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(MobileCenterLog.class);
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(new File(""));
    }

    @Test
    public void constructWrapperSdkExceptionManager() {
        new WrapperSdkExceptionManager();
    }

    @Test
    public void loadWrapperExceptionData() throws Exception {
        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID()));
        doThrow(new ClassNotFoundException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.readObject(any(File.class));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID()));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(null));
    }

    @Test
    public void deleteWrapperExceptionDataWithNullId() {

        /* Delete null does nothing. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(null);
        verifyStatic(never());
        StorageHelper.InternalStorage.delete(any(File.class));
        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithMissingId() {

        /* Delete with file not found does nothing. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic(never());
        StorageHelper.InternalStorage.delete(any(File.class));
        verifyStatic(never());
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithLoadingError() throws Exception {

        /* Delete with file that cannot be loaded because invalid content should just log an error. */
        File file = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(file);
        when(file.exists()).thenReturn(true);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic();
        StorageHelper.InternalStorage.delete(any(File.class));
        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

//    @Test
//    public void saveWrapperExceptionDataNull() throws IOException {
//
//        /* Cannot save with a null uuid */
//        WrapperSdkExceptionManager.saveWrapperExceptionData(null, null);
//        verifyStatic(never());
//        StorageHelper.InternalStorage.writeObject(any(File.class), anyString());
//        verifyStatic();
//        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString());
//    }

//    @Test
//    public void saveWrapperExceptionDataMissingId() throws IOException {
//
//        /* Ok to save null if there is a uuid */
//        WrapperSdkExceptionManager.saveWrapperExceptionData(null, UUID.randomUUID());
//        verifyStatic();
//        StorageHelper.InternalStorage.writeObject(any(File.class), any(Serializable.class));
//        verifyStatic(never());
//        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), any(IOException.class));
//        verifyStatic(never());
//        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString());
//    }
//
//    @Test
//    public void saveWrapperExceptionDataFailing() throws IOException {
//
//        /* Save null and test failure. */
//        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
//        StorageHelper.InternalStorage.writeObject(any(File.class), any(Serializable.class));
//        WrapperSdkExceptionManager.saveWrapperExceptionData(null, UUIDUtils.randomUUID());
//        verifyStatic();
//        StorageHelper.InternalStorage.writeObject(any(File.class), anyString());
//        verifyStatic();
//        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), any(IOException.class));
//    }
}
