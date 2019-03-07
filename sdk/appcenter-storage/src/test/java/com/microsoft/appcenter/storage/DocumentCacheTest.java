package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteQueryBuilder;

import com.microsoft.appcenter.storage.models.BaseOptions;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Test
    public void readReturnsErrorObjectOnDbRuntimeException() {
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        Document<String> doc = mDocumentCache.read("partition", "id", String.class, ReadOptions.CreateNoCacheOption());
        assertNotNull(doc);
        assertNull(doc.getDocument());
        assertTrue(doc.failed());
        assertNotNull(doc.getError());
    }

    @Test
    public void verifyOptionsContstructors() {
        assertEquals(BaseOptions.INFINITE, ReadOptions.CreateInfiniteCacheOption().getDeviceTimeToLive());
        assertEquals(BaseOptions.NO_CACHE, ReadOptions.CreateNoCacheOption().getDeviceTimeToLive());
        assertEquals(BaseOptions.INFINITE, WriteOptions.CreateInfiniteCacheOption().getDeviceTimeToLive());
        assertEquals(BaseOptions.NO_CACHE, WriteOptions.CreateNoCacheOption().getDeviceTimeToLive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyBaseOptionsWithNegativeTtl() {
        ReadOptions readOptions = new ReadOptions(-100);
    }

    @Test
    public void optionsExpirationTest() {
        ReadOptions readOptions = new ReadOptions(1);
        assertTrue(readOptions.isExpired(-1));
    }
}
