package com.microsoft.appcenter.http;

import java.io.IOException;

/**
 *
 */
public class NetworkException extends IOException {
    public NetworkException(Exception e) {
        super(e);
    }
}
