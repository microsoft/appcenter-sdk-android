package com.microsoft.appcenter.analytics;

import android.os.Handler;

import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.TicketCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TicketCache.class, HandlerUtils.class})
public class AuthenticationProviderTest {

    @Before
    public void setUp() {
        mockStatic(TicketCache.class);
    }

    @Test
    public void ticketKeyHash() {
        assertNull(new AuthenticationProvider(null, null, null).getTicketKeyHash());
        assertNotNull(new AuthenticationProvider(null, "test", null).getTicketKeyHash());
    }

    @Test
    public void acquireTokenAsync() {

        /* Mock timer to capture runnable callback to execute the delayed task manually in test. */
        mockStatic(HandlerUtils.class);
        Handler handler = mock(Handler.class);
        when(HandlerUtils.getMainHandler()).thenReturn(handler);
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        when(handler.postDelayed(runnable.capture(), anyLong())).thenReturn(true);

        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        doNothing().when(tokenProvider).getToken(anyString(), callback.capture());
        AuthenticationProvider authenticationProvider = new AuthenticationProvider(null, "key", tokenProvider);

        /* Verify acquireTokenAsync. */
        authenticationProvider.acquireTokenAsync();
        verify(tokenProvider).getToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* Verify onAuthenticationResult. */
        callback.getValue().onAuthenticationResult(null, new Date());
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());
        callback.getValue().onAuthenticationResult("test", null);
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());
        callback.getValue().onAuthenticationResult("test", new Date());
        verifyStatic();
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("test"));

        /* Execute the timer. */
        runnable.getValue().run();

        /* Called a second time. */
        verify(tokenProvider, times(2)).getToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* Check timer stopped on stopping refresh. */
        authenticationProvider.stopRefreshing();
        verify(handler).removeCallbacks(runnable.getValue());

        /* Calling twice stop does nothing more (1 total call). */
        authenticationProvider.stopRefreshing();
        verify(handler).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void typeEnumTest() {

        /* Work around for code coverage. */
        assertEquals(MSA, AuthenticationProvider.Type.valueOf(MSA.name()));
    }
}
