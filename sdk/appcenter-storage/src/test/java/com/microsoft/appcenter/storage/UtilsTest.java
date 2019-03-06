package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.Document;

import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

public class UtilsTest {

    @Test
    public void canParseWhenDocumentMalformed() {
        Document<TestDocument> document = Utils.parseDocument("{}", TestDocument.class);
        assertNotNull(document.getError());
    }
}
