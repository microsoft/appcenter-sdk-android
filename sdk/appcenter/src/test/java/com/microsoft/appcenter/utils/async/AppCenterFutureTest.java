package com.microsoft.appcenter.utils.async;

import com.microsoft.appcenter.utils.HandlerUtils;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class AppCenterFutureTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void getWithInterruption() throws InterruptedException {
        final DefaultAppCenterFuture<Boolean> future = new DefaultAppCenterFuture<>();
        final AtomicReference<Boolean> result = new AtomicReference<>();
        Thread thread = new Thread() {

            @Override
            public void run() {
                result.set(future.get());
            }
        };
        thread.start();
        thread.interrupt();
        future.complete(true);
        thread.join();
        assertEquals(true, result.get());
    }

    @Test
    @PrepareForTest(DefaultAppCenterFuture.class)
    public void isDoneWithInterruption() throws Exception {
        CountDownLatch latch = mock(CountDownLatch.class);
        whenNew(CountDownLatch.class).withAnyArguments().thenReturn(latch);
        when(latch.await(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException()).thenReturn(true);
        final DefaultAppCenterFuture<Boolean> future = new DefaultAppCenterFuture<>();
        final AtomicReference<Boolean> result = new AtomicReference<>();
        Thread thread = new Thread() {

            @Override
            public void run() {
                result.set(future.isDone());
            }
        };
        thread.start();
        thread.join();
        assertEquals(true, result.get());
    }

    @Test
    public void completeTwiceIgnored() {
        DefaultAppCenterFuture<Integer> future = new DefaultAppCenterFuture<>();
        future.complete(1);
        future.complete(2);
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test
    @PrepareForTest(HandlerUtils.class)
    public void multipleCallbacks() {
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        DefaultAppCenterFuture<Integer> future = new DefaultAppCenterFuture<>();

        @SuppressWarnings("unchecked")
        AppCenterConsumer<Integer> function = mock(AppCenterConsumer.class);
        future.thenAccept(function);
        future.thenAccept(function);
        future.complete(1);
        verify(function, times(2)).accept(1);

        /* Works also after completion. */
        future.thenAccept(function);
        future.thenAccept(function);
        verify(function, times(4)).accept(1);
    }
}
