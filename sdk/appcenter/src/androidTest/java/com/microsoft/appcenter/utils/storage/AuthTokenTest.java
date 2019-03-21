package com.microsoft.appcenter.utils.storage;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AuthTokenTest {

    @Test
    public void authTokenTest() {
        String token = UUIDUtils.randomUUID().toString();
        Date startTime = new Date();
        Date endTime = new Date();
        AuthTokenInfo tokenTest = new AuthTokenInfo(token, startTime, endTime);
        assertEquals(tokenTest.getAuthToken(), token);
        assertEquals(tokenTest.getStartTime(), startTime);
        assertEquals(tokenTest.getEndTime(), endTime);
    }

    @Test
    public void authTokenStorageTest() {
        AuthTokenContext tokenContext = new AuthTokenContext();
        AuthTokenStorage authTokenStorage = mock(AuthTokenStorage.class);
        tokenContext.setStorage(authTokenStorage);
        assertEquals(tokenContext.getStorage(), authTokenStorage);
    }

    @Test
    public void cacheAuthTokenTestWithStorageNull() {
        AuthTokenContext tokenContext = new AuthTokenContext();
        String token = tokenContext.getAuthToken();
        String homeId = tokenContext.getHomeAccountId();
        tokenContext.setStorage(null);
        tokenContext.cacheAuthToken();
        assertEquals(tokenContext.getAuthToken(), token);
        assertEquals(tokenContext.getHomeAccountId(), homeId);
    }
}
