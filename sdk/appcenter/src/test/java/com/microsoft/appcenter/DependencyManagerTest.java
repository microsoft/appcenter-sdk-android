/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyNew;

public class DependencyManagerTest extends AbstractAppCenterTest {

    @Test
    public void noSetDependencyCallUsesDefaultHttpClient() throws Exception {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that no services have been auto-loaded since none are configured for this */
        assertTrue(AppCenter.isConfigured());
        verifyNew(DefaultChannel.class).withArguments(any(Context.class), eq(DUMMY_APP_SECRET), any(LogSerializer.class), any(DefaultHttpClient.class), any(Handler.class));
    }

    @Test
    public void setDependencyCallUsesInjectedHttpClient() throws Exception {
        DependencyManager.setDependencies(new MockHttpClient());
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that no services have been auto-loaded since none are configured for this */
        assertTrue(AppCenter.isConfigured());
        verifyNew(DefaultChannel.class).withArguments(any(Context.class), eq(DUMMY_APP_SECRET), any(LogSerializer.class), any(MockHttpClient.class), any(Handler.class));
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
