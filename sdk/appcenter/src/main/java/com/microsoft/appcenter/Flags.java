package com.microsoft.appcenter;

/**
 * Persistence and latency flags for telemetry.
 */
public final class Flags {

    /**
     * Mask for persistence within flags.
     */
    public static final int PERSISTENCE_MASK = 0xFF;

    /**
     * An event can be lost due to low bandwidth or disk space constraints.
     */
    public static final int PERSISTENCE_NORMAL = 0x01;

    /**
     * Used for events that should be prioritized over non-critical events.
     */
    public static final int PERSISTENCE_CRITICAL = 0x02;
}
