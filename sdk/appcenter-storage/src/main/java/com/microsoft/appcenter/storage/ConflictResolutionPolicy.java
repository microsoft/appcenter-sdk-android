package com.microsoft.appcenter.storage;

/**
 * Conflict resolution policy
 */
public class ConflictResolutionPolicy {

    /**
     * Return the default conflict resolution policy: last write wins
     * Any write operation will succeed, potentially overriding concurrent changes
     * @return
     */
    public static ConflictResolutionPolicy getLastWriteWins() {
        return null;
    }

    /**
     * Return the "compare and swap" policy
     * The write operation will only be accepted by the server, if the local document
     * that was previously read is still currently the one the server knows
     * about (i.e. if its etag matches)
     * @param localDocument
     * @param <T>
     * @return
     */
    public static <T> ConflictResolutionPolicy getCompareAndSwap(Document<T> localDocument) {
        return null;
    }

    /**
     * Same as above, but provide a callback to resolve conflicts in case
     * the server rejects an operation
     * @param localDocument
     * @param callback
     * @param <T>
     * @return
     */
    //
    public static <T> ConflictResolutionPolicy geConflictResolution(Document<T> localDocument, ConflictResolutionCallback<T> callback) {
        return null;
    }
}


