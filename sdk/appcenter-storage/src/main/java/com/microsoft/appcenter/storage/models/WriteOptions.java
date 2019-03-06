package com.microsoft.appcenter.storage.models;

public class WriteOptions extends BaseOptions {

    public WriteOptions() { super(); }

    public WriteOptions(int ttl) {
        super(ttl);
    }

    public static WriteOptions CreateInfiniteCacheOption() {
        return new WriteOptions(BaseOptions.INFINITE);
    }

    public static WriteOptions CreateNoCacheOption() {
        return new WriteOptions(BaseOptions.NO_CACHE);
    }
}
