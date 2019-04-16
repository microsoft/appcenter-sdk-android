/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.google.gson.JsonParseException;
import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class UtilsTest {

    @Test
    public void parseDocumentCanHandleInvalidJson() {
        Document<TestDocument> document = Utils.parseDocument("{}", TestDocument.class);
        assertNotNull(document.getDocumentError());
    }

    @Test
    public void parseDocumentsCanHandleInvalidJson() {
        Page<TestDocument> page = Utils.parseDocuments("", TestDocument.class);
        assertNotNull(page.getError());
    }

    @Test
    public void canParseWhenDocumentNull() {
        Document<TestDocument> document = Utils.parseDocument(null, TestDocument.class);
        assertNotNull(document.getDocumentError());
    }

    @Test
    public void canParseWhenPassedWrongType() {
        TestDocument testDoc = new TestDocument("test-value");
        Document<TestDocument> doc = new Document<>(testDoc, "partition", "id");
        Document<String> document = Utils.parseDocument(doc.toString(), String.class);
        assertNotNull(document.getDocumentError());
    }

    @Test
    public void getETag() {
        assertNull(Utils.getEtag(null));
        assertNull(Utils.getEtag(""));
        assertNull(Utils.getEtag("{a:1}"));
    }

    @Test
    public void doesNotAlterReadOnlyPartitionName() {
        assertEquals(Constants.READONLY, Utils.removeAccountIdFromPartitionName(Constants.READONLY));
    }

    @Test
    public void removeAccountIdFromPartitionName() {
        String partition = "user";
        String partitionNameWithAccountId = partition + "-" + "bd45f90e-6eb1-4c47-817e-e59b82b5c03d";
        assertEquals(partition, Utils.removeAccountIdFromPartitionName(partitionNameWithAccountId));
    }

    @Test
    public void checkETagNullByDefault() {
        Document<Void> document = new Document<>(null, "readonly", "id");
        document = Utils.parseDocument(Utils.getGson().toJson(document), Void.class);
        assertNotNull(document);
        assertNull(document.getDocumentError());
        assertNull(document.getEtag());
    }

    @Test
    public void checkIsoDate() {

        /* Serialize a document with a date. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        Document<DateDocument> doc = new Document<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);

        /* Check ISO format. */
        String expectedDate = "1973-11-26T09:13:34.234Z";
        assertTrue(payload.contains(expectedDate));

        /* Check we can parse back. */
        Document<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNull(document.getDocumentError());
        assertNotNull(document.getDocument());
        assertEquals(dateDocument.date, document.getDocument().date);
    }

    @Test
    public void parseInvalidDate() {

        /* Corrupt date format after serialization. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        String expectedDate = "1973-11-26T09:13:34.234Z";
        Document<DateDocument> doc = new Document<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);
        assertTrue(payload.contains(expectedDate));
        payload = payload.replace(expectedDate, "1973/11/26 09:13:34");

        /* Check parsing error. */
        Document<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNotNull(document.getDocumentError());
        assertTrue(document.getDocumentError().getError() instanceof StorageException);
        assertTrue(document.getDocumentError().getError().getCause() instanceof JsonParseException);
        assertNull(document.getDocument());
    }

    private class DateDocument {

        Date date;
    }
}
