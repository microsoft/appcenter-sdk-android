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
     * @deprecated This flag has been deprecated, use {@link #NORMAL} instead.
     */
    @Deprecated
    public static final int PERSISTENCE_NORMAL = 0x01;

    /**
     * An event can be lost due to low bandwidth or disk space constraints.
     */
    public static final int NORMAL = PERSISTENCE_NORMAL;

    /**
     * @deprecated Use of flag PERSISTENCE_CRITICAL has been deprecated, suggest to use new flag CRITICAL as replacement.
     */
    @Deprecated
    public static final int PERSISTENCE_CRITICAL = 0x02;

    /**
     * Used for events that should be prioritized over non-critical events.
     */
    public static final int CRITICAL = PERSISTENCE_CRITICAL;

    /**
     * Default combination of flags.
     */
    public static final int DEFAULTS = NORMAL;

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
        if (persistencePriority != NORMAL && persistencePriority != CRITICAL) {
            if (persistencePriority != 0 && warnFallback) {
                AppCenterLog.warn(LOG_TAG, "Invalid value=" + persistencePriority + " for persistence flag, using NORMAL as a default.");
            }
            persistencePriority = NORMAL;
        }
        return persistencePriority;
    }
}
