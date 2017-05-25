package com.microsoft.azure.mobile;

interface MobileCenterHandler {

    void post(Runnable runnable, boolean runWhenDisabled);
}
