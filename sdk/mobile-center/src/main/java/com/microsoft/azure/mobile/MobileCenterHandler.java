package com.microsoft.azure.mobile;

public interface MobileCenterHandler {

    void post(Runnable runnable, Runnable disabledRunnable);
}
