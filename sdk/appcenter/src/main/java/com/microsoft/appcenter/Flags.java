package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Persistence and latency flags for telemetry.
 */
public final class Flags {

    /**
     * Mask for persistence within flags.
     */
    private static final int PERSISTENCE_MASK = 0xFF;

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
    public static final int DEFAULT_FLAGS = PERSISTENCE_NORMAL;

    /**
     * Get persistence priority flag.
     *
     * @param flags        all flags to extract persistence priority from.
     * @param warnFallback if true and falling back from an invalid value: print a warning.
     * @return persistence priority flag.
     */
    public static int getPersistencePriority(int flags, boolean warnFallback) {
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
