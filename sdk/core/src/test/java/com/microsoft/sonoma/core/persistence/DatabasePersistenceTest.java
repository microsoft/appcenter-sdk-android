package com.microsoft.sonoma.core.persistence;

import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;
import com.microsoft.sonoma.core.utils.SonomaLog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(SonomaLog.class)
public class DatabasePersistenceTest {

    @Test
    public void databaseOperationException() throws Persistence.PersistenceException, IOException {

        /* Mock instances. */
        mockStatic(SonomaLog.class);
        LogSerializer mockSerializer = mock(DefaultLogSerializer.class);
        DatabasePersistence mockPersistence = spy(new DatabasePersistence("test-persistence", "operation.exception", 1));
        doReturn(mockSerializer).when(mockPersistence).getLogSerializer();

        try {
            /* Generate a log and persist. */
            Log log = mock(Log.class);
            mockPersistence.putLog("test-p1", log);
        } finally {
            /* Close. */
            mockPersistence.close();
        }

        verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(RuntimeException.class));
    }
}
