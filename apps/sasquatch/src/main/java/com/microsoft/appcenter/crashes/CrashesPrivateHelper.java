/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;

import java.util.Map;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable, Map<String, String> properties, Iterable<ErrorAttachmentLog> attachments) {
        // TODO add attachments param
        Crashes.trackException(throwable, properties);
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }
}
