/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.auth;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAccountIdentifier;
import com.microsoft.identity.client.IAuthenticationResult;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        Auth.class,
        SystemClock.class,
        SharedPreferencesManager.class,
        FileManager.class,
        AppCenterLog.class,
        AppCenter.class,
        HandlerUtils.class,
        HttpUtils.class,
        AuthTokenContext.class,
        NetworkStateHelper.class
})
abstract public class AbstractAuthTest {

    static final String AUTH_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Auth.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    AppCenterHandler mAppCenterHandler;

    @Mock
    AuthTokenContext mAuthTokenContext;

    @Mock
    NetworkStateHelper mNetworkStateHelper;

    @Mock
    HttpClient mHttpClient;

    @Mock
    private AppCenterFuture<Boolean> mCoreEnabledFuture;

    @Before
    public void setUp() throws Exception {
        Auth.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        when(AppCenter.getLogLevel()).thenReturn(Log.WARN);
        when(AppCenter.isConfigured()).thenReturn(true);
        when(AppCenter.getInstance()).thenReturn(mock(AppCenter.class));
        when(AppCenter.isEnabled()).thenReturn(mCoreEnabledFuture);
        when(mCoreEnabledFuture.get()).thenReturn(true);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        mockStatic(HttpUtils.class);
        when(HttpUtils.createHttpClient(any(Context.class))).thenReturn(mHttpClient);

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(eq(key), eq(true))).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(anyString(), anyBoolean());

        /* Mock file storage. */
        mockStatic(FileManager.class);

        /* Mock token context. */
        AuthTokenContext.unsetInstance();
        whenNew(AuthTokenContext.class).withAnyArguments().thenReturn(mAuthTokenContext);

        /* Mock network state. */
        mockStatic(NetworkStateHelper.class);
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
    }

    IAuthenticationResult mockAuthResult(String idToken, String accessToken, String homeAccountId, String accountId) {
        IAuthenticationResult mockResult = mock(IAuthenticationResult.class);
        when(mockResult.getIdToken()).thenReturn(idToken);
        when(mockResult.getAccessToken()).thenReturn(accessToken);
        IAccount account = mock(IAccount.class);
        IAccountIdentifier homeAccountIdentifier = mock(IAccountIdentifier.class);
        when(homeAccountIdentifier.getIdentifier()).thenReturn(homeAccountId);
        when(account.getHomeAccountIdentifier()).thenReturn(homeAccountIdentifier);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn(accountId);
        when(account.getAccountIdentifier()).thenReturn(accountIdentifier);
        when(mockResult.getAccount()).thenReturn(account);
        when(mockResult.getExpiresOn()).thenReturn(new Date());
        return mockResult;
    }
}
