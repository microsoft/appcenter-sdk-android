package com.microsoft.appcenter.push;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
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
    public void setUp() {
        mockStatic(Push.class);
        when(Push.getInstance()).thenReturn(mPush);
        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);
    }

    @Test
    public void onTokenRefresh() {
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
    public void getApplicationContext() {
        final Context context = mock(Context.class);
        TokenService service = new TokenService() {

            @Override
            public Context getApplicationContext() {
                return context;
            }
        };
        assertSame(context, service.getFirebaseInstanceIdService().getApplicationContext());
    }

    @Test
    public void wrapperCallPassing() throws Exception {

        /* Just check service public method calls are passed to Firebase. */
        TokenService.FirebaseInstanceIdServiceWrapper wrapper = mock(TokenService.FirebaseInstanceIdServiceWrapper.class);
        whenNew(TokenService.FirebaseInstanceIdServiceWrapper.class).withNoArguments().thenReturn(wrapper);
        TokenService service = new TokenService();
        assertSame(wrapper, service.getFirebaseInstanceIdService());
        Intent intent = mock(Intent.class);
        service.onBind(intent);
        verify(wrapper).onBind(intent);
        service.onStartCommand(intent, 0, 1);
        verify(wrapper).onStartCommand(intent, 0, 1);
    }

    @Test
    public void firebaseUnavailable() {

        /* Just check it does not crash when no firebase. */
        when(FirebaseInstanceId.getInstance()).thenReturn(null);
        TokenService service = new TokenService();
        service.onBind(mock(Intent.class));
        service.onStartCommand(mock(Intent.class), 0, 1);
        assertNull(service.getFirebaseInstanceIdService());
    }
}