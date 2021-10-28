/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;
import com.microsoft.appcenter.test.TestUtils;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
public class ReleaseDownloaderFactoryTest {

    @Mock
    private Context mockContext;

    @Mock
    private ReleaseDetails mockReleaseDetails;

    @Mock
    private ReleaseDownloader.Listener mockReleaseDownloaderListener;

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    @SuppressWarnings("ObviousNullCheck")
    @Test
    public void generatedConstructor() {

        /* Coverage fix. */
        assertNotNull(new ReleaseDownloaderFactory());
    }

    @Test
    public void createOnLollipop() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP);
//        ReleaseDownloader releaseDownloader = ReleaseDownloaderFactory.create(mockContext, mockReleaseDetails, mockReleaseDownloaderListener);
//        assertThat(releaseDownloader, instanceOf(DownloadManagerReleaseDownloader.class));
    }
}