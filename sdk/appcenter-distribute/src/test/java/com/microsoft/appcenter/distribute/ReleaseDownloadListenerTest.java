package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.MEBIBYTE_IN_BYTES;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        AppCenter.class,
        AppCenterLog.class,
        Distribute.class,
        HandlerUtils.class,
        InstallerUtils.class,
        ProgressDialog.class,
        ReleaseDetails.class,
        ReleaseDownloadListener.class,
        SharedPreferencesManager.class,
        SystemClock.class
})
public class ReleaseDownloadListenerTest  {

    private static final String DISTRIBUTION_GROUP_ID = "group_id_test";
    private static final String RELEASE_HASH = "release_hash_test";
    private static final int RELEASE_ID = 123;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    Context mContext;

    @Mock
    Activity mActivity;

    @Mock
    private Handler mHandler;

    @Mock
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private android.app.ProgressDialog mProgressDialog;

    @Mock
    Uri mUri;

    @Mock
    private Distribute mockDistribute;

    @Mock
    private Intent mInstallIntent;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() throws Exception {
        mockHandler();
        mockDialog();
        mockStorage();
        mockInstallIntent();
        mockDistribute();
    }

    private void mockDistribute() {
        mockStatic(Distribute.class);
        Mockito.when(Distribute.getInstance()).thenReturn(mockDistribute);
    }

    private void mockInstallIntent() throws Exception {
        whenNew(Intent.class).withArguments(Intent.ACTION_INSTALL_PACKAGE).thenReturn(mInstallIntent);
        when(mInstallIntent.resolveActivity(any(PackageManager.class))).thenReturn(mock(ComponentName.class));
        Mockito.when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    private ReleaseDetails mockReleaseDetails(boolean isMandatory) throws Exception {
        mockStatic(ReleaseDetails.class);
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(RELEASE_ID);
        when(releaseDetails.getDistributionGroupId()).thenReturn(DISTRIBUTION_GROUP_ID);
        when(releaseDetails.getReleaseHash()).thenReturn(RELEASE_HASH);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getDownloadUrl()).thenReturn(mUri);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(isMandatory);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        return releaseDetails;
    }

    private void mockStorage() {
        mockStatic(SharedPreferencesManager.class);

        /* Mock updates to storage. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getLong(invocation.getArguments()[0].toString(), INVALID_DOWNLOAD_IDENTIFIER)).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn((Integer) invocation.getArguments()[1]);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn(DOWNLOAD_STATE_COMPLETED);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getString(invocation.getArguments()[0].toString())).thenReturn(invocation.getArguments()[1].toString());
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_RELEASE_DETAILS), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(SharedPreferencesManager.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);

        /* Mock everything that triggers a download. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
    }

    private void mockHandler() {
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Mock time for Handler.post. */
        mockStatic(SystemClock.class);
        PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(1L);

        /* Mock Handler. */
        PowerMockito.when(HandlerUtils.getMainHandler()).thenReturn(mHandler);
    }

    private void mockDialog() throws Exception {

        /* Mock some dialog methods. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                PowerMockito.when(mProgressDialog.isIndeterminate()).thenReturn((Boolean) invocation.getArguments()[0]);
                return null;
            }
        }).when(mProgressDialog).setIndeterminate(anyBoolean());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                Mockito.when(mProgressDialog.isShowing()).thenReturn(true);
                return null;
            }
        }).when(mProgressDialog).show();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                Mockito.when(mProgressDialog.isShowing()).thenReturn(false);
                return null;
            }
        }).when(mProgressDialog).hide();
        whenNew(android.app.ProgressDialog.class).withAnyArguments().thenReturn(mProgressDialog);
    }

    @Test
    public void onStartTest() throws Exception {
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(true));
        long mockTime = 1000 * 1000 * 1000;

        /* Call onStart(). */
        releaseDownloadListener.onStart(mockTime);

        /* Verify that download state and time are stored on start. */
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_ENQUEUED));
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), eq(mockTime));
    }

    @Test
    public void dontShowDialogOnZeroProgressTest() throws Exception {
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(true));

        /* Setup progressDialog. */
        releaseDownloadListener.showDownloadProgress(mActivity);

        /* Setup invalid totalSize. */
        long totalSize = -1;
        long currentSize = 2;

        /*
        * Verify that progressDialog is not set up with invalid values
        * and the method returns false.
        */
        assertFalse(releaseDownloadListener.onProgress(currentSize, totalSize));
        verify(mProgressDialog, never()).setMax((int)(totalSize / MEBIBYTE_IN_BYTES));
        verify(mProgressDialog, never()).setProgress((int)(currentSize / MEBIBYTE_IN_BYTES));
    }

    @Test
    public void dontShowNullDialogOnProgressTest() throws Exception {

        /* If release is mandatory, the progressDialog is not set and equals to null. */
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(false));

        /* Setup progressDialog. */
        releaseDownloadListener.showDownloadProgress(mActivity);
        long totalSize = 1024 * 1024 * 1024;
        long currentSize = 2;

        /*
        * Verify that if progressDialog is null, it is not set up
        * and the method returns false.
        */
        assertFalse(releaseDownloadListener.onProgress(currentSize, totalSize));
        verify(mProgressDialog, never()).setMax((int)(totalSize / MEBIBYTE_IN_BYTES));
        verify(mProgressDialog, never()).setProgress((int)(currentSize / MEBIBYTE_IN_BYTES));
    }

    @Test
    public void dontHideNullDialogTest() throws Exception {
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(false));

        /* We don't setup progressDialog here by calling showDownloadProgress(). */
        releaseDownloadListener.hideProgressDialog();

        /* Verify that the null progressDialog is not attempted to be hidden. */
        verify(mProgressDialog, never()).hide();
        verify(mHandler, never()).removeCallbacksAndMessages(any());
    }

    @Test
    public void hideDialogTest() throws Exception {
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(true));

        /* Setup progressDialog. */
        releaseDownloadListener.showDownloadProgress(mActivity);

        /* Verify that the dialog is hidden. */
        releaseDownloadListener.hideProgressDialog();
        verify(mProgressDialog, times(1)).hide();
        verify(mHandler).removeCallbacksAndMessages(any());
    }

    @Test
    public void showDialogOnProgressTest() throws Exception {
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails(true));

        /* Setup progressDialog. */
        releaseDownloadListener.showDownloadProgress(mActivity);
        long totalSize = 1024 * 1024 * 1024;
        long currentSize = 2;

        /*
        * Verify that the dialog is set up with the approp[roate values
        * and the method returns true.
        */
        assertTrue(releaseDownloadListener.onProgress(currentSize, totalSize));
        verify(mProgressDialog).setMax((int)(totalSize / MEBIBYTE_IN_BYTES));
        verify(mProgressDialog).setProgress((int)(currentSize / MEBIBYTE_IN_BYTES));
    }

    @Test
    public void onErrorTest() throws Exception {
        ReleaseDetails mockReleaseDetails  = mockReleaseDetails(true);
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails);
        releaseDownloadListener.onError("");

        /* Verify that completeWorkflow() is called on error. */
        verify(mockDistribute).completeWorkflow(mockReleaseDetails);
    }

    @Test
    public void onCompleteTest() throws Exception {
        boolean mandatoryUpdate = false;
        ReleaseDetails mockReleaseDetails = mockReleaseDetails(mandatoryUpdate);

        /* Do not notify the download. */
        when(mockDistribute.notifyDownload(mockReleaseDetails, mInstallIntent)).thenReturn(false);
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails);
        releaseDownloadListener.onComplete(mUri);

        /* Verify that completeWorkflow() is called on not mandatory update. */
        verify(mockDistribute).completeWorkflow(mockReleaseDetails);
        verifyStoreReleaseDetails();
    }

    @Test
    public void onCompleteMandatoryTest() throws Exception {
        boolean mandatoryUpdate = true;
        ReleaseDetails mockReleaseDetails = mockReleaseDetails(mandatoryUpdate);

        /* Do not notify the download. */
        when(mockDistribute.notifyDownload(mockReleaseDetails, mInstallIntent)).thenReturn(false);
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails);
        releaseDownloadListener.onComplete(mUri);

        /* Verify that setInstalling() is called on mandatory update. */
        verify(mockDistribute).setInstalling(mockReleaseDetails);
        verifyStoreReleaseDetails();
    }

    @Test
    public void onCompleteNotifyTest() throws Exception {
        boolean mandatoryUpdate = false;
        ReleaseDetails mockReleaseDetails = mockReleaseDetails(mandatoryUpdate);

        /* Notify the download. */
        when(mockDistribute.notifyDownload(mockReleaseDetails, mInstallIntent)).thenReturn(true);
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails);
        releaseDownloadListener.onComplete(mUri);

        /* Verify that completeWorkflow() and setInstalling() are not called here.*/
        verify(mockDistribute, never()).completeWorkflow(mockReleaseDetails);
        verify(mockDistribute, never()).setInstalling(mockReleaseDetails);
    }

    @Test
    public void onCompleteActivityNotResolvedTest() throws Exception {
        boolean mandatoryUpdate = false;

        /* Mock resolving to null activity. */
        when(mInstallIntent.resolveActivity(any(PackageManager.class))).thenReturn(null);
        ReleaseDetails mockReleaseDetails = mockReleaseDetails(mandatoryUpdate);
        ReleaseDownloadListener releaseDownloadListener = new ReleaseDownloadListener(mContext, mockReleaseDetails);

        /* Verify that nothing is called and the method is exited early with false result. */
        assertFalse(releaseDownloadListener.onComplete(mUri));
        verify(mockDistribute, never()).completeWorkflow(mockReleaseDetails);
        verify(mockDistribute, never()).setInstalling(mockReleaseDetails);
    }

    private void verifyStoreReleaseDetails() {

        /* Verify storing appropriate release details. */
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), eq(DISTRIBUTION_GROUP_ID));
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), eq(RELEASE_HASH));
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), eq(RELEASE_ID));
    }
}
