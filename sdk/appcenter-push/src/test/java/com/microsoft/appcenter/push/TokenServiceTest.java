package com.microsoft.appcenter.push;

import com.google.firebase.iid.FirebaseInstanceId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
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
    Push mPush;

    @Mock
    FirebaseInstanceId mFirebaseInstanceId;

    @Before
    public void setUp() throws Exception {
        mockStatic(Push.class);
        when(Push.getInstance()).thenReturn(mPush);

        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);
    }

    @Test
    public void onTokenRefresh() throws Exception {
        TokenService service = new TokenService();
        when(mFirebaseInstanceId.getToken()).thenReturn(null);
        service.onTokenRefresh();
        verify(mPush, never()).onTokenRefresh(anyString());

        String testToken = "TEST";
        when(mFirebaseInstanceId.getToken()).thenReturn(testToken);
        service.onTokenRefresh();
        verify(mPush).onTokenRefresh(eq(testToken));
    }

}