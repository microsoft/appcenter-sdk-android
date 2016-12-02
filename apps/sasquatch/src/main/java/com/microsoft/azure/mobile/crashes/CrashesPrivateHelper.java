/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.crashes;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable) {
        Crashes.trackException(throwable);
    }
}
