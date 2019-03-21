/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IdHelper.class})
public class DefaultChannelCallbacksTest {

    @Test
    public void onNewTokenIsCalledOnChannel() {
        Ingestion ingestion = mock(Ingestion.class);
        Persistence persistence = mock(Persistence.class);
        mockStatic(IdHelper.class);
        when(IdHelper.getInstallId()).thenReturn(UUIDUtils.randomUUID());
        String mockToken = UUIDUtils.randomUUID().toString();
        new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mock(Handler.class));
        AuthTokenContext.getInstance().setAuthToken(mockToken, "mock-id");
    }

}
