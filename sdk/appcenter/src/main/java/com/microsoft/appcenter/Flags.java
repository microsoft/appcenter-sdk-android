/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Persistence and latency flags for telemetry.
 */
public final class Flags {

    /**
     * An event can be lost due to low bandwidth or disk space constraints.
     */
    public static final int PERSISTENCE_NORMAL = 0x01;

    /**
     * Used for events that should be prioritized over non-critical events.
     */
    public static final int PERSISTENCE_CRITICAL = 0x02;

    /**
     * Default combination of flags.
     */
    public static final int DEFAULTS = PERSISTENCE_NORMAL;

    /**
     * Mask for persistence within flags.
     */
    private static final int PERSISTENCE_MASK = 0xFF;

    /**
     * Get persistence priority flag.
     *
     * @param flags        All flags to extract persistence priority from.
     * @param warnFallback If true and falling back from an invalid value: print a warning.
     * @return persistence Priority flag for persistence.
     */
    public static int getPersistenceFlag(int flags, boolean warnFallback) {
        int persistencePriority = flags & PERSISTENCE_MASK;
        if (persistencePriority != PERSISTENCE_NORMAL && persistencePriority != PERSISTENCE_CRITICAL) {
            if (persistencePriority != 0 && warnFallback) {
                AppCenterLog.warn(LOG_TAG, "Invalid value=" + persistencePriority + " for persistence flag, using NORMAL as a default.");
            }
            persistencePriority = PERSISTENCE_NORMAL;
        }
        return persistencePriority;
    }
}
