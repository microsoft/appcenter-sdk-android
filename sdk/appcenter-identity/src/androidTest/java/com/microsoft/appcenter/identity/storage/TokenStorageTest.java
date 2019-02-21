package com.microsoft.appcenter.identity.storage;

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

        /* Mock token. */
        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(context);
        AuthTokenStorage tokenStorage =  TokenStorageFactory.getTokenStorage(context);
        String mockToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();

        /* Save the token into storage. */
        tokenStorage.saveToken(mockToken, mockAccountId);

        /* Assert that storage returns the same token.*/
        assertEquals(mockToken, tokenStorage.getToken());

        /* Remove the token from storage. */
        tokenStorage.removeToken();

        /* Assert that there's no token in storage. */
        assertNull(tokenStorage.getToken());
    }
}