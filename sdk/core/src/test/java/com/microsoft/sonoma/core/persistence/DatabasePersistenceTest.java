package com.microsoft.sonoma.core.persistence;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;
import com.microsoft.sonoma.core.utils.SonomaLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;

import static com.microsoft.sonoma.core.persistence.DatabasePersistenceAsync.THREAD_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({SonomaLog.class, DatabasePersistenceAsync.class})
public class DatabasePersistenceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

        /* Mock handler for asynchronous persistence */
        HandlerThread mockHandlerThread = mock(HandlerThread.class);
        Looper mockLooper = mock(Looper.class);
        whenNew(HandlerThread.class).withArguments(THREAD_NAME).thenReturn(mockHandlerThread);
        when(mockHandlerThread.getLooper()).thenReturn(mockLooper);
        Handler mockPersistenceHandler = mock(Handler.class);
        whenNew(Handler.class).withArguments(mockLooper).thenReturn(mockPersistenceHandler);
        when(mockPersistenceHandler.post(any(Runnable.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return true;
            }
        });
    }

    @Test
    public void databaseOperationException() throws Persistence.PersistenceException, IOException {

        /* Mock instances. */
        mockStatic(SonomaLog.class);
        LogSerializer mockSerializer = mock(DefaultLogSerializer.class);
        DatabasePersistence mockPersistence = spy(new DatabasePersistence("test-persistence", "operation.exception", 1));
        doReturn(mockSerializer).when(mockPersistence).getLogSerializer();
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));

        try {
            /* Generate a log and persist. */
            Log log = mock(Log.class);
            mockPersistenceAsync.putLog("test-p1", log, null);
        } finally {
            /* Close. */
            //noinspection ThrowFromFinallyBlock
            mockPersistenceAsync.close();
        }

        verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(RuntimeException.class));
    }

    @Test
    public void persistenceCloseException() throws IOException {

        /* Mock instances. */
        DatabasePersistence mockPersistence = spy(new DatabasePersistence("test-persistence", "operation.exception", 1));
        doThrow(new IOException()).when(mockPersistence).close();

        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        DatabasePersistenceAsync.DatabasePersistenceAsyncCallback mockCallback = mock(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class);
        doCallRealMethod().when(mockPersistenceAsync).onFailure(eq(mockCallback), any(Exception.class));

        /* Close without callback. */
        mockPersistenceAsync.close();
        verify(mockCallback, never()).onFailure(any(IOException.class));

        /* Close with callback. */
        mockPersistenceAsync.close(mockCallback);
        verify(mockCallback).onFailure(any(IOException.class));
    }
}
