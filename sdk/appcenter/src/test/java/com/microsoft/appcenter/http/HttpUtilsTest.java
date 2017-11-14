package com.microsoft.appcenter.http;

import junit.framework.Assert;

import org.junit.Test;

import static com.microsoft.appcenter.http.HttpUtils.MAX_CHARACTERS_DISPLAYED_FOR_SECRET;

@SuppressWarnings("unused")
public class HttpUtilsTest {

    @Test
    public void hideNullSecret() {
        Assert.assertNull(HttpUtils.hideSecret(null));
    }

    @Test
    public void hideEmptySecret() {
        Assert.assertEquals("", HttpUtils.hideSecret(""));
    }

    @Test
    public void hideShortSecret() {
        String secret = "Short";

        //noinspection ReplaceAllDot
        Assert.assertEquals(secret.replaceAll(".", "*"), HttpUtils.hideSecret(secret));
    }

    @Test
    public void hideLongSecret() {
        String secret = "This_is_very_long_secret_for_unit_test_in_http_utils";
        String obfuscatedSecret = HttpUtils.hideSecret(secret);
        Assert.assertEquals(secret.length(), obfuscatedSecret.length());
        Assert.assertTrue(obfuscatedSecret.endsWith("*" + secret.substring(secret.length() - MAX_CHARACTERS_DISPLAYED_FOR_SECRET)));
    }
}