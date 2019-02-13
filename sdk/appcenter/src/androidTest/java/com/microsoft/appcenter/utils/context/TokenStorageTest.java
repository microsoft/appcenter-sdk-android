package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TokenStorageTest {

    @Test
    public void testPreferenceTokenStorage() {
        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(context);
        TokenStorage tokenStorage = new PreferenceTokenStorage(context);

        String mockToken = UUIDUtils.randomUUID().toString();
        tokenStorage.saveToken(mockToken);

        assertEquals(mockToken, tokenStorage.getToken());

        tokenStorage.removeToken();

        assertEquals("", tokenStorage.getToken());
    }
}
