/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNew;

public class DependencyConfigurationTest extends AbstractAppCenterTest {

    @Test
    public void ConstructorCoverage() {
        new DependencyConfiguration();
    }

    @Test
    public void noSetDependencyCallUsesDefaultHttpClient() throws Exception {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that the channel was instantiated with default HTTP client. */
        verifyNew(DefaultChannel.class).withArguments(any(Context.class), eq(DUMMY_APP_SECRET), any(LogSerializer.class), isA(HttpClientRetryer.class), any(Handler.class));
    }

    @Test
    public void setDependencyCallUsesInjectedHttpClient() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        DependencyConfiguration.setHttpClient(mockHttpClient);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, (Class<? extends AppCenterService>) null);

        /* Verify that the channel was instantiated with the given HTTP client. */
        verifyNew(DefaultChannel.class).withArguments(any(Context.class), eq(DUMMY_APP_SECRET), any(LogSerializer.class), eq(mockHttpClient), any(Handler.class));
    }
}
