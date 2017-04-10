package com.microsoft.azure.mobile.crashes;


import android.util.Base64;

import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void attachmentWithoutFilename() {
        String text = "Hello World!";
        ErrorAttachmentLog attachment = ErrorAttachments.attachmentWithText(text, null);
        assertNotNull(attachment);
        assertEquals(text, attachment.getData());
        assertNull(attachment.getFileName());
        assertEquals(ErrorAttachments.CONTENT_TYPE_TEXT_PLAIN, attachment.getContentType());
    }

    @Test
    public void validateErrorAttachmentLog(){
        ErrorAttachmentLog emptyLog = null;
        boolean result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
        assertFalse(result);

        {
            emptyLog = mock(ErrorAttachmentLog.class);
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertFalse(result);
        }
        {
            when(emptyLog.getId()).thenReturn(UUID.randomUUID());
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertFalse(result);
        }
        {
            when(emptyLog.getErrorId()).thenReturn(UUID.randomUUID());
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertFalse(result);
        }
        {
            when(emptyLog.getContentType()).thenReturn("1");
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertFalse(result);
        }
        {
            when(emptyLog.getData()).thenReturn("3");
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertTrue(result);
        }
        {
            when(emptyLog.getFileName()).thenReturn(null);
            result = ErrorAttachments.validateErrorAttachmentLog(emptyLog);
            assertTrue(result);
        }
    }

    @Test
    public void isBinaryContentType(){
        boolean result = ErrorAttachments.isBinaryContentType(null);
        assertFalse(result);

        result = ErrorAttachments.isBinaryContentType(ErrorAttachments.CONTENT_TYPE_TEXT_PLAIN);
        assertFalse(result);

        result = ErrorAttachments.isBinaryContentType("image/jpeg");
        assertTrue(result);
    }
}
