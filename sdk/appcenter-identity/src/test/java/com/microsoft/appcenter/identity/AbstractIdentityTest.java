package com.microsoft.appcenter.identity;

import android.os.SystemClock;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({
        Identity.class,
        SystemClock.class,
        SharedPreferencesManager.class,
        FileManager.class,
        AppCenterLog.class,
        AppCenter.class,
        HandlerUtils.class,
        HttpUtils.class,
        AuthTokenContext.class
})
abstract public class AbstractIdentityTest {

    static final String IDENTITY_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Identity.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    AppCenterHandler mAppCenterHandler;

    @Mock
    private AppCenterFuture<Boolean> mCoreEnabledFuture;

    @Mock
    protected AuthTokenContext mAuthTokenContext;

    @Before
    public void setUp() {
        Identity.unsetInstance();
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
        mockStatic(AuthTokenContext.class);
        when(AuthTokenContext.getInstance()).thenReturn(mAuthTokenContext);
    }

    IAuthenticationResult mockAuthResult(String mockIdToken, String mockAccountId) {
        IAuthenticationResult mockResult = Mockito.mock(IAuthenticationResult.class);
        when(mockResult.getAccessToken()).thenReturn("token");
        when(mockResult.getIdToken()).thenReturn(mockIdToken);
        IAccount mockAccount = Mockito.mock(IAccount.class);
        IAccountIdentifier mockIdentifier = Mockito.mock(IAccountIdentifier.class);
        when(mockIdentifier.getIdentifier()).thenReturn(mockAccountId);
        when(mockAccount.getHomeAccountIdentifier()).thenReturn(mockIdentifier);
        when(mockResult.getAccount()).thenReturn(mockAccount);
        return mockResult;
    }
}
