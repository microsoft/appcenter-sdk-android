/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.Intent;
import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class PushIntentUtilsTest {

    @Test
    public void coverInit() {
        new PushIntentUtils();
    }

    @Test
    public void getCustomData() {
        Map<String, String> customData = new HashMap<>();
        customData.put("some key", "some value");
        customData.put("some key2", "some value2");
        final Map<String, String> extras = new HashMap<>(customData);
        extras.put(PushIntentUtils.EXTRA_COLOR, "val");
        extras.put(PushIntentUtils.EXTRA_SOUND_ALT, "val");
        extras.put(PushIntentUtils.EXTRA_GOOGLE_MESSAGE_ID, "val");
        extras.put(PushIntentUtils.EXTRA_ICON, "val");
        extras.put(PushIntentUtils.EXTRA_MESSAGE, "val");
        extras.put(PushIntentUtils.EXTRA_SOUND, "val");
        extras.put(PushIntentUtils.EXTRA_TITLE, "val");
        extras.put(PushIntentUtils.EXTRA_GOOGLE_PREFIX + "ttl", "val");
        for (String key : PushIntentUtils.EXTRA_STANDARD_KEYS) {
            extras.put(key, "val");
        }

        /* Create a mock bundle that will actually use a map to store values. */
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundle.keySet()).thenReturn(extras.keySet());
        when(mockBundle.get(anyString())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                String key = (String) invocation.getArguments()[0];
                return extras.get(key);
            }
        });
        Intent pushIntent = mock(Intent.class);
        when(pushIntent.getExtras()).thenReturn(mockBundle);
        Map<String, String> retrievedCustomData = PushIntentUtils.getCustomData(pushIntent);
        assertEquals(customData, retrievedCustomData);
    }

    @Test
    public void getCustomDataFromNullIntentExtras() {
        assertEquals(Collections.emptyMap(), PushIntentUtils.getCustomData(mock(Intent.class)));
    }

    @Test
    public void getTitle() {
        String pushTitle = "push title";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_TITLE, pushTitle);
        String retrievedTitle = PushIntentUtils.getTitle(pushIntent);
        assertEquals(pushTitle, retrievedTitle);
    }

    @Test
    public void getMessage() {
        String pushMessage = "push message";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_MESSAGE, pushMessage);
        String retrievedMessage = PushIntentUtils.getMessage(pushIntent);
        assertEquals(pushMessage, retrievedMessage);
    }

    @Test
    public void getGoogleMessageId() {
        String messageId = "message id";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_GOOGLE_MESSAGE_ID, messageId);
        String retrievedMessageId = PushIntentUtils.getMessageId(pushIntent);
        assertEquals(messageId, retrievedMessageId);
    }

    @Test
    public void setGoogleMessageId() {
        String messageId = "message id";
        Intent pushIntent = mock(Intent.class);
        PushIntentUtils.setMessageId(messageId, pushIntent);
        verify(pushIntent).putExtra(PushIntentUtils.EXTRA_GOOGLE_MESSAGE_ID, messageId);
    }

    @Test
    public void noSound() {
        assertNull(PushIntentUtils.getSound(mock(Intent.class)));
    }

    @Test
    public void getSoundWithOldKey() {
        String sound = "sound";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND, "this is not the sound you are looking for");
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND_ALT, sound);
        String retrievedSound = PushIntentUtils.getSound(pushIntent);
        assertEquals(sound, retrievedSound);
    }

    @Test
    public void getSound() {
        String sound = "sound";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND, "sound");
        String retrievedSound = PushIntentUtils.getSound(pushIntent);
        assertEquals(sound, retrievedSound);
    }

    @Test
    public void getColor() {
        String color = "color";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_COLOR, color);
        String retrievedColor = PushIntentUtils.getColor(pushIntent);
        assertEquals(color, retrievedColor);
    }

    @Test
    public void getIcon() {
        String icon = "3";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_ICON, icon);
        String retrievedIcon = PushIntentUtils.getIcon(pushIntent);
        assertEquals(icon, retrievedIcon);
    }

    private static void mockPutExtra(Intent intentMock, String key, String value) {
        when(intentMock.getStringExtra(key)).thenReturn(value);
    }
}
