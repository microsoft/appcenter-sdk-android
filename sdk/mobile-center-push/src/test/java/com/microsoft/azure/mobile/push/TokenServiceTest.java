package com.microsoft.azure.mobile.push;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

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
    public void setUp() throws Exception {
        mockStatic(Push.class);
        when(Push.getInstance()).thenReturn(mPush);
        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);
    }

    @Test
    public void onTokenRefresh() throws Exception {
        TokenService service = new TokenService();
        FirebaseInstanceIdService firebaseInstanceIdService = service.getFirebaseInstanceIdService();
        assertNotNull(firebaseInstanceIdService);
        when(mFirebaseInstanceId.getToken()).thenReturn(null);
        firebaseInstanceIdService.onTokenRefresh();
        verify(mPush).onTokenRefresh(null);
        String testToken = "TEST";
        when(mFirebaseInstanceId.getToken()).thenReturn(testToken);
        firebaseInstanceIdService.onTokenRefresh();
        verify(mPush).onTokenRefresh(eq(testToken));
    }

    @Test
    public void getApplicationContextWrapper() {
        final Context context = mock(Context.class);
        TokenService service = new TokenService() {

            @Override
            public Context getApplicationContext() {
                return context;
            }
        };
        assertSame(context, service.getFirebaseInstanceIdService().getApplicationContext());
    }
}