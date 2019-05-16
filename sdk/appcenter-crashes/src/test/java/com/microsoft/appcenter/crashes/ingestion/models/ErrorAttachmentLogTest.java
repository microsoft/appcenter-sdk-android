/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import org.junit.Test;

import java.util.UUID;

import static com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog.CHARSET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class ErrorAttachmentLogTest {

    @Test
    public void attachmentWithText() {
        String text = "Hello World!";
        String fileName = "1";
        ErrorAttachmentLog attachment = ErrorAttachmentLog.attachmentWithText(text, fileName);
        assertNotNull(attachment);
        assertEquals(text, new String(attachment.getData(), CHARSET));
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
        assertEquals(data, attachment.getData());
        assertEquals(fileName, attachment.getFileName());
        assertEquals(contentType, attachment.getContentType());
    }

    @Test
    public void attachmentWithoutFilename() {
        String text = "Hello World!";
        ErrorAttachmentLog attachment = ErrorAttachmentLog.attachmentWithText(text, null);
        assertNotNull(attachment);
        assertEquals(text, new String(attachment.getData(), CHARSET));
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
            log.setData("3".getBytes(CHARSET));
            assertTrue(log.isValid());
        }
        {
            log.setFileName(null);
            assertTrue(log.isValid());
        }
    }
}
