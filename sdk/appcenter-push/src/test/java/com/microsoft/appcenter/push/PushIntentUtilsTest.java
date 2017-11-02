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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class PushIntentUtilsTest {

    @Test
    public void coverInit() {
        assertNotNull(new PushIntentUtils());
    }

    @Test
    public void getCustomData() {
        Map<String, String> customData = new HashMap<>();
        customData.put("some key", "some value");
        customData.put("some key2", "some value2");
        final Map<String, String> extras = new HashMap<>();
        extras.putAll(customData);
        extras.put(PushIntentUtils.EXTRA_COLOR, "val");
        extras.put(PushIntentUtils.EXTRA_CUSTOM_SOUND, "val");
        extras.put(PushIntentUtils.EXTRA_GOOGLE_MESSAGE_ID, "val");
        extras.put(PushIntentUtils.EXTRA_ICON, "val");
        extras.put(PushIntentUtils.EXTRA_MESSAGE, "val");
        extras.put(PushIntentUtils.EXTRA_SOUND, "val");
        extras.put(PushIntentUtils.EXTRA_TITLE, "val");
        for (String key : PushIntentUtils.EXTRA_STANDARD_KEYS) {
            extras.put(key, "val");
        }

        /* Create a mock bundle that will actually use a map to store values. */
        Bundle mockBundle = mock(Bundle.class);
        when(mockBundle.keySet()).thenReturn(extras.keySet());
        when(mockBundle.getString(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
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
        String retrievedMessageId = PushIntentUtils.getGoogleMessageId(pushIntent);
        assertEquals(messageId, retrievedMessageId);
    }

    @Test
    public void setGoogleMessageId() {
        String messageId = "message id";
        Intent pushIntent = mock(Intent.class);
        PushIntentUtils.setGoogleMessageId(messageId, pushIntent);
        verify(pushIntent).putExtra(PushIntentUtils.EXTRA_GOOGLE_MESSAGE_ID, messageId);
    }

    @Test
    public void getCustomSound() {
        String sound = "sound";
        Intent pushIntent = mock(Intent.class);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_CUSTOM_SOUND, sound);
        String retrievedSound = PushIntentUtils.getCustomSound(pushIntent);
        assertEquals(sound, retrievedSound);
    }

    @Test
    public void useAnySound() {
        Intent pushIntent = mock(Intent.class);

        /* Case 1: neither sound nor sound2 are set. */
        boolean useAnySound = PushIntentUtils.useAnySound(pushIntent);
        assertFalse(useAnySound);

        /* Case 2: only sound is set. */
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND, "default");
        useAnySound = PushIntentUtils.useAnySound(pushIntent);
        assertTrue(useAnySound);

        /* Case 3: only sound2 is set. */
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND, null);
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_CUSTOM_SOUND, "custom_sound");
        useAnySound = PushIntentUtils.useAnySound(pushIntent);
        assertTrue(useAnySound);

        /* Case 4: both sound and sound2 are set. */
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_SOUND, "default");
        mockPutExtra(pushIntent, PushIntentUtils.EXTRA_CUSTOM_SOUND, "custom_sound");
        useAnySound = PushIntentUtils.useAnySound(pushIntent);
        assertTrue(useAnySound);
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
