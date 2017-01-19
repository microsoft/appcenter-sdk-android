package com.microsoft.azure.mobile.utils;

import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class HandlerUtilsTest {

    @Test
    public void runOnUiThread() {

        /* Constructor code coverage is needed... */
        new HandlerUtils();
        final AtomicReference<Thread> mainThreadFirstRun = new AtomicReference<>();
        final AtomicReference<Thread> mainThreadNestedRun = new AtomicReference<>();
        final Semaphore semaphore = new Semaphore(0);
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mainThreadFirstRun.set(Thread.currentThread());
                HandlerUtils.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mainThreadNestedRun.set(Thread.currentThread());
                        semaphore.release();
                    }
                });
            }
        });
        semaphore.acquireUninterruptibly();
        assertNotNull(mainThreadFirstRun.get());
        assertEquals(mainThreadFirstRun.get(), mainThreadNestedRun.get());
        assertNotEquals(Thread.currentThread(), mainThreadNestedRun.get());
    }
}
