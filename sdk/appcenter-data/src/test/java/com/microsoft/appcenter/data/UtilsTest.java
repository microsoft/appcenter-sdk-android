/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.TokenResult;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class UtilsTest {

    @Test
    public void parseDocumentCanHandleInvalidJson() {
        DocumentWrapper<TestDocument> document = Utils.parseDocument("{}", TestDocument.class);
        assertNotNull(document.getError());
    }

    @Test
    public void parseDocumentsCanHandleInvalidJson() {
        Page<TestDocument> page = Utils.parseDocuments("", TestDocument.class);
        assertNotNull(page.getError());
    }

    @Test
    public void canParseWhenWrapperIsNull() {
        DocumentWrapper<TestDocument> document = Utils.parseDocument(null, TestDocument.class);
        assertNotNull(document.getError());
    }

    @Test
    public void canParseWhenDocumentIsNull() {
        DocumentWrapper<String> wrapper = new DocumentWrapper<>(null, "partition", "doc_id");
        String serializedDocument = wrapper.getJsonValue();
        String serializedWrapper = wrapper.toString();
        DocumentWrapper<String> deserializedWrapper = Utils.parseDocument(serializedWrapper, String.class);

        assertNull(serializedDocument);
        assertTrue(serializedWrapper.contains("\"document\":null"));
        assertNull(deserializedWrapper.getDeserializedValue());
    }

    @Test
    public void canParseWhenDocumentHasNullValues() {
        Map<String, String> doc = new HashMap<>();
        //noinspection ConstantConditions
        doc.put("key", null);
        DocumentWrapper<Map<String, String>> wrapper = new DocumentWrapper<>(doc, "partition", "doc_id");
        String serializedDocument = wrapper.getJsonValue();
        String serializedWrapper = wrapper.toString();
        DocumentWrapper<Map> deserializedWrapper = Utils.parseDocument(serializedWrapper, Map.class);
        Map deserializedDoc = deserializedWrapper.getDeserializedValue();

        assertEquals("{\"key\":null}", serializedDocument);
        assertTrue(serializedWrapper.contains("\"document\":{\"key\":null}"));
        assertEquals(doc, deserializedDoc);

    }

    @Test
    public void canParseWhenPassedWrongType() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", "id");
        DocumentWrapper<String> document = Utils.parseDocument(doc.toString(), String.class);
        assertNotNull(document.getError());
    }

    @Test
    public void jsonValueIsUserDocument() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", "id");
        assertNotNull(doc.getJsonValue());
        assertEquals(doc.getJsonValue(), Utils.getGson().toJson(testDoc));
        assertNotEquals(doc.getJsonValue(), doc.toString());
        assertFalse(doc.hasFailed());
    }

    @Test
    public void jsonValueForNullDocument() {
        Void deletedDocument = null;
        DocumentWrapper<Void> doc = new DocumentWrapper<>(deletedDocument, "partition", "id");
        assertNull(doc.getJsonValue());
        assertEquals("partition", doc.getPartition());
        assertEquals("id", doc.getId());
        assertFalse(doc.hasFailed());
    }

    @Test
    public void getETag() {
        assertNull(Utils.getETag(null));
        assertNull(Utils.getETag(""));
        assertNull(Utils.getETag("{a:1}"));
    }

    @Test
    public void localDocumentExpired() {
        LocalDocument localDocument = new LocalDocument(DefaultPartitions.APP_DOCUMENTS, null, "user", "test", "test", TimeToLive.INFINITE, 0, 0);
        assertFalse(localDocument.isExpired());
    }

    @Test
    public void doesNotAlterReadOnlyPartitionName() {
        assertEquals(DefaultPartitions.APP_DOCUMENTS, Utils.removeAccountIdFromPartitionName(DefaultPartitions.APP_DOCUMENTS));
    }

    @Test
    public void removeAccountIdFromPartitionName() {
        String partition = "user";
        String partitionNameWithAccountId = partition + "-" + "bd45f90e-6eb1-4c47-817e-e59b82b5c03d";
        assertEquals(partition, Utils.removeAccountIdFromPartitionName(partitionNameWithAccountId));
    }

    @Test
    public void checkETagNullByDefault() {
        DocumentWrapper<Void> document = new DocumentWrapper<>(null, "readonly", "id");
        document = Utils.parseDocument(Utils.getGson().toJson(document), Void.class);
        assertNotNull(document);
        assertNull(document.getError());
        assertNull(document.getETag());
    }

    @Test
    public void checkIsoDate() {

        /* Serialize a document with a date. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        DocumentWrapper<DateDocument> doc = new DocumentWrapper<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);

        /* Check ISO format. */
        String expectedDate = "1973-11-26T09:13:34.234Z";
        assertTrue(payload.contains(expectedDate));

        /* Check we can parse back. */
        DocumentWrapper<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNull(document.getError());
        assertNotNull(document.getDeserializedValue());
        assertEquals(dateDocument.date, document.getDeserializedValue().date);
    }

    @Test
    public void parseInvalidDate() {

        /* Corrupt date format after serialization. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        String expectedDate = "1973-11-26T09:13:34.234Z";
        DocumentWrapper<DateDocument> doc = new DocumentWrapper<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);
        assertTrue(payload.contains(expectedDate));
        payload = payload.replace(expectedDate, "1973/11/26 09:13:34");

        /* Check parsing error. */
        DocumentWrapper<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNotNull(document.getError());
        assertTrue(document.getError().getCause() instanceof JsonParseException);
        assertNull(document.getDeserializedValue());
        assertNull(document.getJsonValue());
        assertEquals("partition", document.getPartition());
        assertEquals("id", document.getId());
    }

    @Test
    public void missingPartition() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, null, "id");
        doc = Utils.parseDocument(Utils.getGson().toJson(doc), TestDocument.class);
        assertNotNull(doc.getError());

        /*
         * The error is corrupted payload from cosmos DB or SQLite, so other metadata not set when
         * a required metadata field is missing.
         */
        assertNull(doc.getId());
    }

    @Test
    public void missingDocumentId() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", null);
        doc = Utils.parseDocument(Utils.getGson().toJson(doc), TestDocument.class);
        assertNotNull(doc.getError());

        /*
         * The error is corrupted payload from cosmos DB or SQLite, so other metadata not set when
         * a required metadata field is missing.
         */
        assertNull(doc.getPartition());
    }

    @Test
    public void missingTimestamp() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", "id");
        JsonObject corruptedPayload = Utils.getGson().toJsonTree(doc).getAsJsonObject();
        corruptedPayload.remove("_ts");
        doc = Utils.parseDocument(Utils.getGson().toJson(corruptedPayload), TestDocument.class);
        assertNotNull(doc.getError());

        /*
         * The error is corrupted payload from cosmos DB, so other metadata not set when
         * a required metadata field is missing.
         */
        assertNull(doc.getPartition());
        assertNull(doc.getId());
    }

    @Test
    public void isValidTokenResult() {
        TokenResult result = new TokenResult();
        assertFalse(Utils.isValidTokenResult(result));
        result.setDbAccount("dbAccount");
        assertFalse(Utils.isValidTokenResult(result));
        result.setDbName("dbName");
        assertFalse(Utils.isValidTokenResult(result));
        result.setDbAccount("accountName");
        assertFalse(Utils.isValidTokenResult(result));
        result.setDbCollectionName("collectionName");
        assertFalse(Utils.isValidTokenResult(result));
        result.setToken("token");
        assertTrue(Utils.isValidTokenResult(result));
    }

    private class DateDocument {

        Date date;
    }
}
