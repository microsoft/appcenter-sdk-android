package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Context;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.HashUtils;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import junit.framework.Assert;

import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static com.microsoft.azure.mobile.updates.UpdateConstants.LOGIN_PAGE_URL_PATH;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_PLATFORM;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_URI;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class UpdatesTest extends AbstractUpdatesTest {

    @Test
    public void singleton() {
        Assert.assertSame(Updates.getInstance(), Updates.getInstance());
    }

    @Test
    public void storeTokenBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);

        /* Store token before start, start in background, no storage access. */
        Updates.getInstance().storeUpdateToken("some token", "r");
        Updates.getInstance().onStarted(mock(Context.class), "", mock(Channel.class));
        verifyStatic(never());
        PreferencesStorage.putString(anyString(), anyString());
        verifyStatic(never());
        PreferencesStorage.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void storeTokenFromBrowserSameProcess() throws Exception {

        /* Setup mock. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());

        /* Start and resume: open browser. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Activity activity = mock(Activity.class);
        Updates.getInstance().onActivityResumed(activity);
        verifyStatic();
        String url = UpdateConstants.DEFAULT_LOGIN_URL;
        url += String.format(LOGIN_PAGE_URL_PATH, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + HashUtils.sha256("com.contoso:1.2.3:6");
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token and verify. */
        Updates.getInstance().storeUpdateToken("some token", requestId.toString());
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }
}
