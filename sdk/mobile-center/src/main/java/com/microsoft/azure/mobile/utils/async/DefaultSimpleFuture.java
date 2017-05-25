package com.microsoft.azure.mobile.utils.async;

import com.microsoft.azure.mobile.utils.HandlerUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DefaultSimpleFuture<T> implements SimpleFuture<T> {

    private final CountDownLatch mLatch = new CountDownLatch(1);

    private T mResult;

    private Collection<SimpleFunction<T>> mFunctions;

    @Override
    public T get() {
        while (true) {
            try {
                mLatch.await();
                break;
            } catch (InterruptedException ignored) {
            }
        }
        return mResult;
    }

    @Override
    public boolean isDone() {
        while (true) {
            try {
                return mLatch.await(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public synchronized SimpleFuture<T> thenApply(final SimpleFunction<T> function) {
        if (isDone()) {
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    function.apply(mResult);
                }
            });
        } else {
            if (mFunctions == null) {
                mFunctions = new LinkedList<>();
            }
            mFunctions.add(function);
        }
        return this;
    }

    public synchronized void complete(final T value) {
        if (!isDone()) {
            mResult = value;
            mLatch.countDown();
            if (mFunctions != null) {
                HandlerUtils.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        /* No need to synchronize anymore as mFunctions cannot be modified anymore. */
                        for (SimpleFunction<T> function : mFunctions) {
                            function.apply(value);
                        }
                        mFunctions = null;
                    }
                });
            }
        }
    }

}
