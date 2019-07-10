/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("WeakerAccess")
@PrepareForTest({
        AppCenterLog.class,
        AuthTokenContext.class,
        DefaultChannel.class,
        DeviceInfoHelper.class,
        HandlerUtils.class,
        IdHelper.class,
        SharedPreferencesManager.class,
        System.class
})
public class AbstractDefaultChannelTest {

    static final String TEST_GROUP = "group_test";

    static final long BATCH_TIME_INTERVAL = 500;

    static final int MAX_PARALLEL_BATCHES = 3;

    static final String MOCK_TOKEN = UUID.randomUUID().toString();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    protected Handler mAppCenterHandler;

    @Mock
    protected AuthTokenContext mAuthTokenContext;

    static Answer<String> getGetLogsAnswer() {
        return getGetLogsAnswer(-1);
    }

    static Answer<String> getGetLogsAnswer(final int size) {
        return new Answer<String>() {

            @Override
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int length = size >= 0 ? size : (int) args[2];
                if (args[3] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[3];
                    for (int i = 0; i < length; i++) {
                        logs.add(mock(Log.class));
                    }
                }
                return length > 0 ? UUID.randomUUID().toString() : null;
            }
        };
    }

    static Answer<Object> getSendAsyncAnswer() {
        return getSendAsyncAnswer(null);
    }

    static Answer<Object> getSendAsyncAnswer(final Exception e) {
        return new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                if (args[4] instanceof ServiceCallback) {
                    if (e == null)
                        ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("", null);
                    else
                        ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(e);
                }
                return null;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        mockStatic(AppCenterLog.class);
        mockStatic(IdHelper.class, new Returns(UUID.randomUUID()));
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mock(Device.class));
        when(mAppCenterHandler.post(any(Runnable.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return true;
            }
        });
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        mockStatic(AuthTokenContext.class);
        when(mAuthTokenContext.getAuthToken()).thenReturn(MOCK_TOKEN);
        when(mAuthTokenContext.getAuthTokenValidityList()).thenReturn(Collections.singletonList(new AuthTokenInfo()));
        when(AuthTokenContext.getInstance()).thenReturn(mAuthTokenContext);
        whenNew(AuthTokenContext.class).withAnyArguments().thenReturn(mAuthTokenContext);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(System.class);
    }
}
