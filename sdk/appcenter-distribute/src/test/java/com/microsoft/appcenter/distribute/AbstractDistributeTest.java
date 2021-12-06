/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.channel.DistributeInfoTracker;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

import java.util.UUID;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.utils.PrefStorageConstants.ALLOWED_NETWORK_REQUEST;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("CanBeFinal")
@PrepareForTest({
        AppCenter.class,
        AppCenterLog.class,
        AppNameHelper.class,
        BrowserUtils.class,
        CryptoUtils.class,
        Distribute.class,
        DistributeUtils.class,
        HandlerUtils.class,
        HttpUtils.class,
        IdHelper.class,
        InstallerUtils.class,
        NetworkStateHelper.class,
        ReleaseDetails.class,
        ReleaseDownloaderFactory.class,
        SharedPreferencesManager.class,
        TextUtils.class,
        Toast.class
})
public class AbstractDistributeTest {

    static final String TEST_HASH = HashUtils.sha256("com.contoso:1.2.3:6");

    public static final String DISTRIBUTE_ENABLED_KEY = KEY_ENABLED + "_Distribute";

    private static final String LOCAL_FILENAME_PATH_MOCK = "ANSWER_IS_42";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    /**
     * Use a timeout to fail test if deadlocks happen due to a code change.
     */
    @Rule
    public Timeout mGlobalTimeout = Timeout.seconds(10);

    @Mock
    Context mContext;

    @Mock
    Activity mActivity;

    @Mock
    PackageManager mPackageManager;

    @Mock
    ApplicationInfo mApplicationInfo;

    @Mock
    AlertDialog.Builder mDialogBuilder;

    @Mock
    AlertDialog mDialog;

    @Mock
    Toast mToast;

    NetworkStateHelper mNetworkStateHelper;

    @Mock
    CryptoUtils mCryptoUtils;

    @Mock
    AppCenterHandler mAppCenterHandler;

    @Mock
    Channel mChannel;

    @Mock
    DistributeInfoTracker mDistributeInfoTracker;

    @Mock
    HttpClient mHttpClient;

    @Mock
    ReleaseDownloader mReleaseDownloader;

    @Mock
    ReleaseDownloadListener mReleaseDownloaderListener;

    @Mock
    ReleaseInstallerListener mReleaseInstallerListener;

    @Mock
    ReleaseDetails mReleaseDetails;

    @Mock
    Uri mUri;

    @Mock
    Intent mInstallIntent;

    @Mock
    NotificationManager mNotificationManager;

    UUID mInstallId = UUID.randomUUID();

    @Before
    @SuppressLint("ShowToast")
    @SuppressWarnings("ResourceType")
    public void setUp() throws Exception {
        Distribute.unsetInstance();
        mockStatic(AppCenterLog.class);
        mockStatic(AppCenter.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mAppCenterHandler).post(any(Runnable.class), any(Runnable.class));
        whenNew(DistributeInfoTracker.class).withAnyArguments().thenReturn(mDistributeInfoTracker);
        mockStatic(IdHelper.class);
        when(IdHelper.getInstallId()).thenReturn(mInstallId);

        /* First call to com.microsoft.appcenter.AppCenter.isEnabled shall return true, initial state. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(DISTRIBUTE_ENABLED_KEY, true)).thenReturn(true);
        when(SharedPreferencesManager.getBoolean(ALLOWED_NETWORK_REQUEST, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(DISTRIBUTE_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(eq(DISTRIBUTE_ENABLED_KEY), anyBoolean());

        /* Default download id when not found. */
        when(SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Mock package manager. */
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageName()).thenReturn("com.contoso");
        when(mActivity.getPackageName()).thenReturn("com.contoso");
        when(mContext.getApplicationInfo()).thenReturn(mApplicationInfo);
        when(mActivity.getApplicationInfo()).thenReturn(mApplicationInfo);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        PackageInfo packageInfo = mock(PackageInfo.class);
        when(mPackageManager.getPackageInfo("com.contoso", 0)).thenReturn(packageInfo);
        Whitebox.setInternalState(packageInfo, "packageName", "com.contoso");
        Whitebox.setInternalState(packageInfo, "versionName", "1.2.3");
        Whitebox.setInternalState(packageInfo, "versionCode", 6);
        Whitebox.setInternalState(packageInfo, "lastUpdateTime", 2);

        /* Mock app name and other string resources. */
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContext)).thenReturn("unit-test-app");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_optional)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_update_dialog_message_mandatory)).thenReturn("%s%s%d");
        when(mContext.getString(R.string.appcenter_distribute_install_ready_message)).thenReturn("%s%s%d");

        /* Mock network. */
        mockStatic(NetworkStateHelper.class);
        mNetworkStateHelper = mock(NetworkStateHelper.class, new Returns(true));
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
        spy(HttpUtils.class);
        doReturn(mHttpClient).when(HttpUtils.class, "createHttpClient", any(Context.class));

        /* Mock some statics. */
        mockStatic(BrowserUtils.class);
        mockStatic(TextUtils.class);
        mockStatic(InstallerUtils.class);
        when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence str = (CharSequence) invocation.getArguments()[0];
                return str == null || str.length() == 0;
            }
        });

        /* Mock Crypto to not crypt. */
        mockStatic(CryptoUtils.class);
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);
        when(mCryptoUtils.decrypt(anyString())).thenAnswer(new Answer<CryptoUtils.DecryptedData>() {

            @Override
            public CryptoUtils.DecryptedData answer(InvocationOnMock invocation) {
                Object arg = invocation.getArguments()[0];
                return new CryptoUtils.DecryptedData(arg == null ? null : arg.toString(), null);
            }
        });
        when(mCryptoUtils.encrypt(anyString())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) {
                return invocation.getArguments()[0].toString();
            }
        });

        /* Dialog. */
        whenNew(AlertDialog.Builder.class).withAnyArguments().thenReturn(mDialogBuilder);
        when(mDialogBuilder.create()).thenReturn(mDialog);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(mDialog.isShowing()).thenReturn(true);
                return null;
            }
        }).when(mDialog).show();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(mDialog.isShowing()).thenReturn(false);
                return null;
            }
        }).when(mDialog).hide();

        /* Toast. */
        mockStatic(Toast.class);
        when(Toast.makeText(any(Context.class), anyInt(), anyInt())).thenReturn(mToast);

        /* Mock Handler .*/
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Mock Release Details. */
        mockStatic(ReleaseDetails.class);
        when(ReleaseDetails.parse(anyString())).thenReturn(mReleaseDetails);

        /* Mock Release Downloader. */
        mockStatic(ReleaseDownloaderFactory.class);
        when(ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloadListener.class))).thenReturn(mReleaseDownloader);
        when(mReleaseDownloader.getReleaseDetails()).thenReturn(mReleaseDetails);

        /* Mock Release Downloader Listener. */
        mReleaseDownloaderListener = spy(new ReleaseDownloadListener(mContext, mReleaseDetails));
        whenNew(ReleaseDownloadListener.class).withArguments(any(Context.class), any(ReleaseDetails.class)).thenReturn(mReleaseDownloaderListener);

        /* Mock Release Installer Listener. */
        mReleaseInstallerListener = mock(ReleaseInstallerListener.class);
        whenNew(ReleaseInstallerListener.class).withArguments(any(Context.class)).thenReturn(mReleaseInstallerListener);

        /* Mock Uri. */
        when(mUri.toString()).thenReturn(LOCAL_FILENAME_PATH_MOCK);
        when(mUri.getPath()).thenReturn(LOCAL_FILENAME_PATH_MOCK);
        when(mUri.getEncodedPath()).thenReturn(LOCAL_FILENAME_PATH_MOCK);

        /* Mock Install Intent. */
        when(mInstallIntent.getData()).thenReturn(mUri);
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(mInstallIntent);
    }

    void restartProcessAndSdk() {
        Distribute.unsetInstance();
        start();
    }

    void start() {
        Distribute.getInstance().onStarting(mAppCenterHandler);
        Distribute.getInstance().onStarted(mContext, mChannel, "a", null, true);
    }

    /* Shared code to mock a restart of an activity considered to be the launcher. */
    void restartResumeLauncher(Activity activity) {
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(activity.getClass().getName());
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(activity);
    }
}
