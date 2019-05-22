/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import com.google.firebase.iid.FirebaseInstanceId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({
        TokenService.class,
        Push.class,
        FirebaseInstanceId.class
})
public class TokenServiceTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Push mPush;

    @Mock
    private FirebaseInstanceId mFirebaseInstanceId;

    @Before
    public void setUp() {
        mockStatic(Push.class);
        when(Push.getInstance()).thenReturn(mPush);
        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);
    }

    @Test
    public void onTokenRefresh() {
        TokenService service = new TokenService();
        service.onNewToken(null);
        verify(mPush).onTokenRefresh(null);
        String testToken = "TEST";
        service.onNewToken(testToken);
        verify(mPush).onTokenRefresh(eq(testToken));
    }
}