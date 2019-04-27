/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.content.Context;

import org.junit.Test;

import static com.microsoft.appcenter.http.HttpUtils.MAX_CHARACTERS_DISPLAYED_FOR_SECRET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unused")
public class HttpUtilsTest {

    @Test
    public void hideEmptySecret() {
        assertEquals("", HttpUtils.hideSecret(""));
    }

    @Test
    public void hideShortSecret() {
        assertEquals("*****", HttpUtils.hideSecret("Short"));
    }

    @Test
    public void hideLongSecret() {
        String secret = "This_is_very_long_secret_for_unit_test_in_http_utils";
        String obfuscatedSecret = HttpUtils.hideSecret(secret);
        assertEquals(secret.length(), obfuscatedSecret.length());
        assertTrue(obfuscatedSecret.endsWith("*" + secret.substring(secret.length() - MAX_CHARACTERS_DISPLAYED_FOR_SECRET)));
    }

    @Test
    public void hide1ApiKey() {
        String key = "bb8b06dfcd8f4b4f925cca3e980fcdc4-a8b05ade-4dd6-4fd1-8dc7-c76b0cbc6bdf-1234";
        String hiddenKeys = HttpUtils.hideApiKeys(key);
        assertEquals("bb8b06dfcd8f4b4f925cca3e980fcdc4-***", hiddenKeys);
    }

    @Test
    public void hide2ApiKeys() {
        String key1 = "bb8b06dfcd8f4b4f925cca3e980fcdc4-a8b05ade-4dd6-4fd1-8dc7-c76b0cbc6bdf-1234";
        String key2 = "6721516c7c754b49ba4ec5e7170a0c4c-a5f44609-30df-4fae-9202-49836a6cf71d-4567";
        String keys = key1 + "," + key2;
        String hiddenKeys = HttpUtils.hideApiKeys(keys);
        assertEquals("bb8b06dfcd8f4b4f925cca3e980fcdc4-***,6721516c7c754b49ba4ec5e7170a0c4c-***", hiddenKeys);
    }

    @Test
    public void hideEmptyApiKeys() {
        assertEquals("", HttpUtils.hideApiKeys(""));
    }

    @Test
    public void hideInvalidApiKey() {
        assertEquals("asIs", HttpUtils.hideApiKeys("asIs"));
    }

    @Test
    public void hideTickets() {
        assertEquals("{\"a\":\"d:***\",\"b\":\"p:***\"}", HttpUtils.hideTickets("{\"a\":\"d:mock1\",\"b\":\"p:mock2\"}"));
    }

    @Test
    public void hideEmptyTicket() {
        assertEquals("", HttpUtils.hideTickets(""));
    }

    @Test
    public void hideInvalidTicket() {
        assertEquals("asIs", HttpUtils.hideTickets("asIs"));
    }

    @Test
    public void hideAuthToken() {
        String token = "Bearer jwt-token-string";
        String hiddenToken = HttpUtils.hideAuthToken(token);
        assertEquals(hiddenToken, "Bearer ***");
    }

    @Test
    public void defaultCompressionSettings() {
        HttpClient httpClient = HttpUtils.createHttpClient(mock(Context.class));
        DefaultHttpClient defaultHttpClient = getDefaultHttpClient((HttpClientDecorator) httpClient);
        assertTrue(defaultHttpClient.isCompressionEnabled());
    }

    @Test
    public void enabledCompressionSettings() {
        HttpClient httpClient = HttpUtils.createHttpClient(mock(Context.class), true);
        DefaultHttpClient defaultHttpClient = getDefaultHttpClient((HttpClientDecorator) httpClient);
        assertTrue(defaultHttpClient.isCompressionEnabled());
    }

    @Test
    public void disabledCompressionSettings() {
        HttpClient httpClient = HttpUtils.createHttpClient(mock(Context.class), false);
        DefaultHttpClient defaultHttpClient = getDefaultHttpClient((HttpClientDecorator) httpClient);
        assertFalse(defaultHttpClient.isCompressionEnabled());
    }

    private DefaultHttpClient getDefaultHttpClient(HttpClientDecorator httpClientDecorator) {
        httpClientDecorator = (HttpClientDecorator) httpClientDecorator.getDecoratedApi();
        return (DefaultHttpClient) httpClientDecorator.getDecoratedApi();
    }
}