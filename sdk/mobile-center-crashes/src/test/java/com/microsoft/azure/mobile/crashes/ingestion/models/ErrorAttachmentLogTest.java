package com.microsoft.azure.mobile.crashes.ingestion.models;


import android.util.Base64;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"unused", "ConstantConditions"})
public class ErrorAttachmentLogTest {

    @Test
    public void attachmentWithText() {
        String text = "Hello World!";
        String fileName = "1";
        ErrorAttachmentLog attachment = ErrorAttachmentLog.attachmentWithText(text, fileName);
        assertNotNull(attachment);
        assertEquals(text, attachment.getData());
        assertEquals(fileName, attachment.getFileName());
        assertEquals(ErrorAttachmentLog.CONTENT_TYPE_TEXT_PLAIN, attachment.getContentType());
    }

    @Test
    public void attachmentWithBinary() {
        byte[] data = "Hello Binary!".getBytes();
        String fileName = "binary.txt";
        String contentType = "image/jpeg";
        ErrorAttachmentLog attachment = ErrorAttachmentLog.attachmentWithBinary(data, fileName, contentType);
        assertNotNull(attachment);
        assertEquals(Base64.encodeToString(data, Base64.DEFAULT), attachment.getData());
        assertEquals(fileName, attachment.getFileName());
        assertEquals(contentType, attachment.getContentType());
    }

    @Test
    public void attachmentWithoutFilename() {
        String text = "Hello World!";
        ErrorAttachmentLog attachment = ErrorAttachmentLog.attachmentWithText(text, null);
        assertNotNull(attachment);
        assertEquals(text, attachment.getData());
        assertNull(attachment.getFileName());
        assertEquals(ErrorAttachmentLog.CONTENT_TYPE_TEXT_PLAIN, attachment.getContentType());
    }

    @Test
    public void validateErrorAttachmentLog(){
        ErrorAttachmentLog log;
        {
            log = new ErrorAttachmentLog();
            assertFalse(log.isValid());
        }
        {
            log.setId(UUID.randomUUID());
            assertFalse(log.isValid());
        }
        {
            log.setErrorId(UUID.randomUUID());
            assertFalse(log.isValid());
        }
        {
            log.setContentType("1");
            assertFalse(log.isValid());
        }
        {
            log.setData("3");
            assertTrue(log.isValid());
        }
        {
            log.setFileName(null);
            assertTrue(log.isValid());
        }
    }

    @Test
    public void isBinaryContentType(){
        boolean result = ErrorAttachmentLog.isBinaryContentType(null);
        assertFalse(result);

        result = ErrorAttachmentLog.isBinaryContentType(ErrorAttachmentLog.CONTENT_TYPE_TEXT_PLAIN);
        assertFalse(result);

        result = ErrorAttachmentLog.isBinaryContentType("image/jpeg");
        assertTrue(result);
    }
}
