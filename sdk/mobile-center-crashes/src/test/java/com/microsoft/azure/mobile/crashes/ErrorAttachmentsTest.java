package com.microsoft.azure.mobile.crashes;


import android.util.Base64;

import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unused")
public class ErrorAttachmentsTest {

    @Test
    public void utilsPatternCoverage() {
        new ErrorAttachments();
    }

    @Test
    public void attachmentWithText() {
        String text = "Hello World!";
        String fileName = "1";
        ErrorAttachmentLog attachment = ErrorAttachments.attachmentWithText(text, fileName);
        assertNotNull(attachment);
        assertEquals(text, attachment.getData());
        assertEquals(fileName, attachment.getFileName());
        assertEquals(ErrorAttachments.CONTENT_TYPE_TEXT_PLAIN, attachment.getContentType());
    }

    @Test
    public void attachmentWithBinary() {
        byte[] data = "Hello Binary!".getBytes();
        String fileName = "binary.txt";
        String contentType = "image/jpeg";
        ErrorAttachmentLog attachment = ErrorAttachments.attachmentWithBinary(data, fileName, contentType);
        assertNotNull(attachment);
        assertEquals(Base64.encodeToString(data, Base64.DEFAULT), attachment.getData());
        assertEquals(fileName, attachment.getFileName());
        assertEquals(contentType, attachment.getContentType());
    }
}
