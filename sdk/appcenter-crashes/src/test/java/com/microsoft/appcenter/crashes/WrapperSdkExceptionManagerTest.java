package com.microsoft.appcenter.crashes;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({AppCenter.class, WrapperSdkExceptionManager.class, AppCenterLog.class, StorageHelper.PreferencesStorage.class, StorageHelper.InternalStorage.class, Crashes.class, ErrorLogHelper.class, HandlerUtils.class})
public class WrapperSdkExceptionManagerTest {

    private static final String CRASHES_ENABLED_KEY = KEY_ENABLED + "_" + Crashes.getInstance().getServiceName();

    @Rule
    public final PowerMockRule rule = new PowerMockRule();

    @Rule
    public final TemporaryFolder errorStorageDirectory = new TemporaryFolder();

    @Before
    public void setUp() {
        Crashes.unsetInstance();
        mockStatic(AppCenter.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(AppCenterLog.class);
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(errorStorageDirectory.getRoot());
        ManagedErrorLog errorLogMock = mock(ManagedErrorLog.class);
        when(errorLogMock.getId()).thenReturn(UUID.randomUUID());
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong(), anyBoolean()))
                .thenReturn(errorLogMock);

        @SuppressWarnings("unchecked")
        AppCenterFuture<Boolean> future = (AppCenterFuture<Boolean>) mock(AppCenterFuture.class);
        when(AppCenter.isEnabled()).thenReturn(future);
        when(future.get()).thenReturn(true);
        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

        /* Mock handlers. */
        mockStatic(HandlerUtils.class);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        AppCenterHandler handler = mock(AppCenterHandler.class);
        Crashes.getInstance().onStarting(handler);
        doAnswer(runNow).when(handler).post(any(Runnable.class), any(Runnable.class));
    }

    @Test
    public void constructWrapperSdkExceptionManager() {
        new WrapperSdkExceptionManager();
    }

    @Test
    public void loadWrapperExceptionData() throws java.lang.Exception {
        File file = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(file);
        when(file.exists()).thenReturn(true);
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
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithMissingId() {

        /* Delete with file not found does nothing. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic(never());
        StorageHelper.InternalStorage.delete(any(File.class));
        verifyStatic(never());
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithLoadingError() throws java.lang.Exception {

        /* Delete with file that cannot be loaded because invalid content should just log an error. */
        File file = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(file);
        when(file.exists()).thenReturn(true);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic();
        StorageHelper.InternalStorage.delete(any(File.class));
        verifyStatic();
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void saveWrapperSdkCrash() throws JSONException, IOException {
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        byte[] data = new byte[]{'d'};
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(data));

        /* We can't do it twice in the same process. */
        data = new byte[]{'e'};
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(never());
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(data));
    }

    @Test
    public void saveWrapperSdkCrashWithJavaThrowable() throws JSONException, IOException {
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        byte[] data = new byte[]{'d'};
        Throwable throwable = new Throwable();
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), data);
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(data));
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(throwable));

        /* We can't do it twice in the same process. */
        data = new byte[]{'e'};
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), data);
        verifyStatic(never());
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(data));
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(throwable));
    }

    @Test
    public void saveWrapperSdkCrashWithOnlyJavaThrowable() throws JSONException, IOException {
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        Throwable throwable = new Throwable();
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), null);
        verifyStatic(never());
        StorageHelper.InternalStorage.writeObject(any(File.class), isNull(byte[].class));
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(throwable));

        /* We can't do it twice in the same process. */
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), null);
        verifyStatic();
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(throwable));
    }

    @Test
    public void saveWrapperSdkCrashFailsToCreateThrowablePlaceholder() throws java.lang.Exception {
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        File throwableFile = mock(File.class);
        whenNew(File.class).withParameterTypes(String.class, String.class).withArguments(anyString(), argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return String.valueOf(argument).endsWith(ErrorLogHelper.THROWABLE_FILE_EXTENSION);
            }
        })).thenReturn(throwableFile);
        when(throwableFile.createNewFile()).thenReturn(false);
        byte[] data = new byte[]{'d'};
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));

        /* Second call is ignored. */
        data = new byte[]{'e'};
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);

        /* No more error. */
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));
    }

    @Test
    public void saveWrapperSdkCrashFailsWithJSONException() throws JSONException {
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenThrow(new JSONException("mock"));
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'d'});
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof JSONException;
            }
        }));

        /* Second call is ignored. */
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'e'});

        /* No more error. */
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof JSONException;
            }
        }));
    }

    @Test
    public void saveWrapperSdkCrashFailsWithIOException() throws IOException, JSONException {
        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.write(any(File.class), anyString());
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'d'});
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));

        /* Second call is ignored. */
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'e'});

        /* No more error. */
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));
    }

    @Test
    public void saveWrapperSdkCrashFailsWithIOExceptionAfterLog() throws IOException, JSONException {
        byte[] data = {'d'};
        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.writeObject(any(File.class), eq(data));
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));

        /* Second call is ignored. */
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'e'});

        /* No more error. */
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), argThat(new ArgumentMatcher<Throwable>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof IOException;
            }
        }));
    }

    @Test
    public void saveWrapperExceptionWhenSDKDisabled() throws JSONException {
        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        LogSerializer logSerializer = Mockito.mock(LogSerializer.class);
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'d'});
        verify(logSerializer, never()).serializeLog(any(Log.class));
        verifyNoMoreInteractions(ErrorLogHelper.class);
    }
}
