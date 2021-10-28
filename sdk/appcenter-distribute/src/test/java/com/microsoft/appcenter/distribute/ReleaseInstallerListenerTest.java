package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;

@PrepareForTest({
        AppCenter.class,
        AppCenterLog.class,
        Distribute.class,
        ProgressDialog.class,
        HandlerUtils.class,
})
public class ReleaseInstallerListenerTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Distribute mDistribute;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private PackageInstaller mPackageInstaller;

    @Mock
    private InstallerUtils mInstallerUtils;

    private ReleaseInstallerListener mReleaseInstallerListener;

    public ReleaseInstallerListenerTest() {
    }

    @Before
    public void setUp() throws FileNotFoundException {
        ParcelFileDescriptor mockFileDescriptor = mock(ParcelFileDescriptor.class);
        when(mockFileDescriptor.getFileDescriptor()).thenReturn(mock(FileInputStream.class));
        mReleaseInstallerListener = spy(new ReleaseInstallerListener(mContext));
        mockStatic(AppCenterLog.class);
        when(mDownloadManager.openDownloadedFile(Matchers.<Long>any())).thenReturn(mockFileDescriptor);
        when(mContext.getSystemService(anyString())).thenReturn(mDownloadManager);
    }

    @Test
    public void onCreatedTest() {

        verifyStatic();
        AppCenterLog.debug(eq(AppCenterLog.LOG_TAG), eq("The install session was created."));
    }

    @Test
    public void onProgressChangedTest() {
        float mProgress = 15.5F;
        int mSessionId = 10;
        int mDownloadId = 15;
        //TODO: mock ProgressDialog construct
        //TODO: ReleaseInstallerListener.showInstallProgressDialog()

        //TODO: mReleaseInstallerListener.setDownloadId(mDownloadId)
        //TODO: mockInstallerUtils.installPackage()(посмотреть capture, который перехватывает параметры у метода(listener))
        //TODO: mReleaseInstallerListener.startInstall(mDownload)
        //TODO: call methods listener(onCreate, activityChange, onProgress, e.t.c)

        mReleaseInstallerListener.onProgressChanged(mSessionId, mProgress);

        verifyStatic();
        AppCenterLog.verbose(LOG_TAG, eq(String.format(Locale.ENGLISH, "Installing %d of %d done.", (int)(mProgress * 100), 100)));

        verify();
    }




}
