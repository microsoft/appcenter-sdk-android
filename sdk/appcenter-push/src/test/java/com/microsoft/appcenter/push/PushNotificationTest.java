/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PushIntentUtils.class)
public class PushNotificationTest {

    @Test
    public void testInitializationNoIntent() {
        String title = "title";
        String message = "message";
        Map<String, String> customData = new HashMap<>();
        customData.put("key", "value");
        PushNotification notification = new PushNotification("title", "message", customData);
        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
        assertEquals(customData, notification.getCustomData());
    }

    @Test
    public void testInitializationWithIntent() {
        String title = "title";
        String message = "message";
        Map<String, String> customData = new HashMap<>();
        customData.put("key", "value");
        Intent pushIntent = mock(Intent.class);
        mockStatic(PushIntentUtils.class);
        when(PushIntentUtils.getTitle(pushIntent)).thenReturn(title);
        when(PushIntentUtils.getMessage(pushIntent)).thenReturn(message);
        when(PushIntentUtils.getCustomData(pushIntent)).thenReturn(customData);
        PushNotification notification = new PushNotification(pushIntent);
        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
        assertEquals(customData, notification.getCustomData());
    }
}
