package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TokenStorageTest {

    @Test
    public void testPreferenceTokenStorage() {
        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(context);
        ITokenStorage tokenStorage = new PreferenceTokenStorage(context);

        String mockToken = UUIDUtils.randomUUID().toString();
        tokenStorage.saveToken(mockToken);

        assertEquals(mockToken, tokenStorage.getToken());

        tokenStorage.removeToken();

        assertNull(tokenStorage.getToken());
    }
}
