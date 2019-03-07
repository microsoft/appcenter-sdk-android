package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;

import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SQLiteUtils.class, AppCenterLog.class, DatabaseManager.class, DocumentCache.class})
public class DocumentCacheTest {

    private DatabaseManager mDatabaseManager;
    private DocumentCache mDocumentCache;

    @Before
    public void setUp() throws Exception {
        mockStatic(AppCenterLog.class);
        mDatabaseManager = mock(DatabaseManager.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);
        mDocumentCache = new DocumentCache(mock(Context.class));
    }

    @Test
    public void upsertGetsCalledInWrite() {
        mDocumentCache.write(new Document<>("Test value", "partition", "id"), new WriteOptions());
        ArgumentCaptor<ContentValues> values = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).upsert(values.capture());
    }
}
