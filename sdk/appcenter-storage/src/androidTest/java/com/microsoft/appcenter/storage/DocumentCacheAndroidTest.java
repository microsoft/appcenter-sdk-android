/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class DocumentCacheAndroidTest {

    private static final String TEST_VALUE = "Test value";

    private static final String PARTITION = "partition";

    private static final String ID = "id";

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    private DocumentCache mDocumentCache;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
    }

    @Before
    public void setUp() {
        mDocumentCache = new DocumentCache(sContext);
    }

    @AfterClass
    public static void tearDownClass() {
        sContext.deleteDatabase(DocumentCache.DATABASE);
    }

    @Test
    public void writeReadDelete() {
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mDocumentCache.write(document, new WriteOptions());
        Document<String> cachedDocument = mDocumentCache.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(cachedDocument);
        assertEquals(document.getDocument(), cachedDocument.getDocument());
        assertFalse(document.failed());
        assertFalse(document.isFromCache());
        assertTrue(cachedDocument.isFromCache());
        mDocumentCache.delete(PARTITION, ID);
        Document<String> deletedDocument = mDocumentCache.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getError());
    }

    @Test
    public void readExpiredDocument() {

        /* Write a document and mock device ttl to be already expired a few seconds ago. */
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mDocumentCache.write(document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -2;
            }
        });

        /* Read with a TTL of 1 second: already expired. */
        Document<String> deletedDocument = mDocumentCache.read(PARTITION, ID, String.class, new ReadOptions(1));
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getError());
    }
}
