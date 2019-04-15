/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.Document;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class UtilsTest {

    @Test
    public void canParseWhenDocumentMalformed() {
        Document<TestDocument> document = Utils.parseDocument("{}", TestDocument.class);
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
}
