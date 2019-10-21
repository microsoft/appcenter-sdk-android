/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }
}
