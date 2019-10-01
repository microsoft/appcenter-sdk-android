/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.http.HttpConnectionReleaseDownloader;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;
import com.microsoft.appcenter.test.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
public class ReleaseDownloaderFactoryTest {

    private Context mockContext;

    private ReleaseDetails mockReleaseDetails;

    private ReleaseDownloader.Listener mockReleaseDownloaderListener;

    @Before
    public void setUp() {
        mockContext = mock(Context.class);
        mockReleaseDetails = mock(ReleaseDetails.class);
        mockReleaseDownloaderListener = mock(ReleaseDownloader.Listener.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    @Test
    public void createOnLollipop() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 21);
        ReleaseDownloader releaseDownloader = ReleaseDownloaderFactory.create(mockContext, mockReleaseDetails, mockReleaseDownloaderListener);
        assertThat(releaseDownloader, instanceOf(DownloadManagerReleaseDownloader.class));
    }

    @Test
    public void createOn4thAndroid() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 16);
        ReleaseDownloader releaseDownloader = ReleaseDownloaderFactory.create(mockContext, mockReleaseDetails, mockReleaseDownloaderListener);
        assertThat(releaseDownloader, instanceOf(HttpConnectionReleaseDownloader.class));
    }
}