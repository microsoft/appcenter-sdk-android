/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;

import com.microsoft.appcenter.storage.models.BaseOptions;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.DocumentError;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({
        SQLiteUtils.class,
        AppCenterLog.class,
        DatabaseManager.class,
        LocalDocumentStorage.class})
public class LocalDocumentStorageTest {

    private static final String PARTITION = "partition";

    private static final String DOCUMENT_ID = "id";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private DatabaseManager mDatabaseManager;

    private LocalDocumentStorage mLocalDocumentStorage;

    private Cursor mCursor;

    @Before
    public void setUp() throws Exception {
        mockStatic(AppCenterLog.class);
        mDatabaseManager = mock(DatabaseManager.class);
        mCursor = mock(Cursor.class);
        whenNew(DatabaseManager.class).withAnyArguments().thenReturn(mDatabaseManager);
        mLocalDocumentStorage = new LocalDocumentStorage(mock(Context.class));
    }

    @Test
    public void updateGetsCalledInWrite() {
        mLocalDocumentStorage.write(new Document<>("Test value", PARTITION, DOCUMENT_ID), new WriteOptions());
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(argumentCaptor.capture());
        assertNotNull(argumentCaptor.getValue());
    }

    @Test
    public void localStorageDoNotWriteWhenNotCache() {
        mLocalDocumentStorage.write(new Document<>("Test", PARTITION, DOCUMENT_ID), new WriteOptions(WriteOptions.NO_CACHE));
        verify(mDatabaseManager, never()).replace(any(ContentValues.class));
    }

    @Test
    public void updateGetsCalledInWriteWithPendingOperation() {
        mLocalDocumentStorage.write(new Document<>("Test value", PARTITION, DOCUMENT_ID), new WriteOptions(), null);
        ArgumentCaptor<ContentValues> argumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mDatabaseManager).replace(argumentCaptor.capture());
        assertNotNull(argumentCaptor.getValue());
    }

    @Test
    public void readReturnsErrorObjectOnDbRuntimeException() {
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        Document<String> doc = mLocalDocumentStorage.read(PARTITION, DOCUMENT_ID, String.class, ReadOptions.CreateNoCacheOption());
        assertNotNull(doc);
        assertNull(doc.getDocument());
        assertTrue(doc.failed());
        assertEquals(DocumentError.class, doc.getError().getClass());
        assertThat(doc.getError().getError().getMessage(), CoreMatchers.containsString("Failed to read from cache."));
    }

    @Test
    public void createOrUpdateReturnErrorOnDbRuntimeException(){
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenThrow(new RuntimeException());
        Document<String> doc = mLocalDocumentStorage.createOrUpdate(PARTITION, DOCUMENT_ID,"test", String.class, new WriteOptions());
        assertNotNull(doc);
        assertNull(doc.getDocument());
        assertTrue(doc.failed());
        assertEquals(DocumentError.class, doc.getError().getClass());
        assertThat(doc.getError().getError().getMessage(), CoreMatchers.containsString("Failed to read from cache."));
    }

    @Test
    public void createOrUpdateFailedToWriteException() {
        when(mDatabaseManager.getCursor(any(SQLiteQueryBuilder.class), any(String[].class), any(String[].class), anyString())).thenReturn(mCursor);
        when(mDatabaseManager.nextValues(mCursor)).thenReturn(null);
        when(mDatabaseManager.replace(any(ContentValues.class))).thenReturn(-1L);
        Document<String> doc = mLocalDocumentStorage.createOrUpdate(PARTITION, DOCUMENT_ID,"test", String.class, new WriteOptions());
        assertNotNull(doc);
        assertNotNull(doc.getError().getError());
    }

    @Test
    public void deleteReturnsErrorObjectOnDbRuntimeException() {
        doThrow(new RuntimeException()).when(mDatabaseManager).delete(anyString(), any(String[].class));
        mLocalDocumentStorage.delete(PARTITION, DOCUMENT_ID);
        verify(mDatabaseManager).delete(anyString(), AdditionalMatchers.aryEq(new String[]{PARTITION, DOCUMENT_ID}));
    }

    @Test
    public void verifyOptionsConstructors() {
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
