/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ReleaseDetailsTest {

    @Test
    public void getPropertyFromReleaseDetailsTest() {
        ReleaseDetails releaseDetails = new ReleaseDetails();
        assertEquals(releaseDetails.getDistributionGroupId(), "distributionGroupId");
        assertNull(releaseDetails.getDownloadUrl());
        assertEquals(releaseDetails.getId(), 1);
        assertEquals(releaseDetails.getMinApiLevel(), 0);
        assertEquals(releaseDetails.getReleaseHash(), "releaseHash");
        assertEquals(releaseDetails.getReleaseNotes(), "releaseNotes");
        assertNull(releaseDetails.getReleaseNotesUrl());
        assertEquals(releaseDetails.getShortVersion(), "shortVersion");
        assertEquals(releaseDetails.getSize(), 0);
        assertEquals(releaseDetails.getVersion(), 1);
        assertFalse(releaseDetails.isMandatoryUpdate());
    }
}
