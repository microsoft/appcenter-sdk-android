package com.microsoft.sonoma.errors;

import com.microsoft.sonoma.errors.model.ErrorAttachment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        ErrorAttachment attachment = ErrorAttachments.attachmentWithText(text);
        assertNotNull(attachment);
        assertEquals(text, attachment.getTextAttachment());
        assertEquals(attachment, ErrorAttachments.attachment(text, null, null, null));
    }

    @Test
    public void attachmentWithNullText() {
        assertNull(ErrorAttachments.attachmentWithText(null));
        assertNull(ErrorAttachments.attachment(null, null, null, null));
    }

    @Test
    public void attachmentWithBinary() {
        byte[] data = "Hello Binary!".getBytes();
        String fileName = "binary.txt";
        String contentType = "text/plain";
        ErrorAttachment attachment = ErrorAttachments.attachmentWithBinary(data, fileName, contentType);
        assertNotNull(attachment);
        assertNotNull(attachment.getBinaryAttachment());
        assertEquals(data, attachment.getBinaryAttachment().getData());
        assertEquals(fileName, attachment.getBinaryAttachment().getFileName());
        assertEquals(contentType, attachment.getBinaryAttachment().getContentType());
        assertEquals(attachment, ErrorAttachments.attachment(null, data, fileName, contentType));
    }

    @Test
    public void attachmentWithNullBinary() {
        String fileName = "binary.txt";
        String contentType = "text/plain";
        {
            assertNull(ErrorAttachments.attachmentWithBinary(null, fileName, contentType));
            assertNull(ErrorAttachments.attachmentWithBinary(null, null, contentType));
            assertNull(ErrorAttachments.attachmentWithBinary(null, fileName, null));
            assertNull(ErrorAttachments.attachmentWithBinary(null, null, null));
        }
        {
            assertNull(ErrorAttachments.attachment(null, null, fileName, contentType));
            assertNull(ErrorAttachments.attachment(null, null, null, contentType));
            assertNull(ErrorAttachments.attachment(null, null, fileName, null));
            assertNull(ErrorAttachments.attachment(null, null, null, null));
        }
    }

    @Test
    public void attachmentWithTextAndBinary() {
        String text = "Hello World!";
        byte[] data = "Hello Binary!".getBytes();
        String fileName = "binary.txt";
        String contentType = "text/plain";
        ErrorAttachment attachment = ErrorAttachments.attachment(text, data, fileName, contentType);
        assertNotNull(attachment);
        assertEquals(text, attachment.getTextAttachment());
        assertNotNull(attachment.getBinaryAttachment());
        assertEquals(data, attachment.getBinaryAttachment().getData());
        assertEquals(fileName, attachment.getBinaryAttachment().getFileName());
        assertEquals(contentType, attachment.getBinaryAttachment().getContentType());
    }

    @Test
    public void attachmentWithTextAndNullBinary() {
        String text = "Hello World!";
        String fileName = "binary.txt";
        String contentType = "text/plain";
        ErrorAttachment attachment = ErrorAttachments.attachment(text, null, null, null);
        assertNotNull(attachment);
        assertEquals(text, attachment.getTextAttachment());
        assertNull(attachment.getBinaryAttachment());
        assertEquals(attachment, ErrorAttachments.attachment(text, null, fileName, null));
        assertEquals(attachment, ErrorAttachments.attachment(text, null, null, contentType));
        assertEquals(attachment, ErrorAttachments.attachment(text, null, fileName, contentType));
    }
}
