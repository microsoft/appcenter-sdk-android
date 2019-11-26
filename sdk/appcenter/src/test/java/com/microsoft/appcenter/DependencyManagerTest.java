/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.junit.Test;

import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.powermock.api.mockito.PowerMockito.verifyNew;

public class DependencyManagerTest extends AbstractAppCenterTest {

    @Test
    public void noSetDependencyCallUsesDefaultHttpClient() throws Exception {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that the channel was instantiated with default HTTP client. */
        verifyNew(DefaultChannel.class).withArguments(isA(Context.class), eq(DUMMY_APP_SECRET), isA(LogSerializer.class), isA(MockHttpClient.class), isA(Handler.class));
    }

    @Test
    public void setDependencyCallUsesInjectedHttpClient() throws Exception {
        HttpClient mockHttpClient = new MockHttpClient();
        DependencyManager.setDependencies(mockHttpClient);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that the channel was instantiated with the given HTTP client. */
        verifyNew(DefaultChannel.class).withArguments(isA(Context.class), eq(DUMMY_APP_SECRET), isA(LogSerializer.class), eq(mockHttpClient), isA(Handler.class));
    }

    private static class MockHttpClient implements HttpClient {

        @Override
        public ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
            return null;
        }

        @Override
        public void reopen() {
        }

        @Override
        public void close() {
        }
    }
}
