/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Most of the public vs private scenarios were already tested in existing test classes
 * before we introduced the switch.
 * This test class focuses on the getter and switching the value.
 */
public class DistributeUpdateTrackTest extends AbstractDistributeTest {

    @Test
    public void getAndSetValueBeforeStart() {

        /* Check default value. */
        Assert.assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());

        /* Check changes. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
        Distribute.setUpdateTrack(UpdateTrack.PUBLIC);
        Assert.assertEquals(UpdateTrack.PUBLIC, Distribute.getUpdateTrack());
    }

    @Test
    public void setInvalidUpdateTrack() {

        /* Set a valid non default value. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Try set invalid values. */
        Distribute.setUpdateTrack(0);
        Distribute.setUpdateTrack(-1);
        Distribute.setUpdateTrack(42);

        /* Check value didn't change. */
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
    }

    @Test
    public void persistValueWhenDisabled() {

        /* Start and disable SDK. */
        start();
        Distribute.setEnabled(false).get();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Switch track. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Verify it's persisted. */
        Distribute.unsetInstance();
        start();
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());

        /* Verify no check for update call (or browser). */
        verifyNoMoreInteractions(mHttpClient);
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void persistValueWhenSwitchBeforeStart() {

        /* Switch track before start. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);

        /* Start. */
        start();

        /* Restart. */
        Distribute.unsetInstance();
        start();

        /* Check. */
        Assert.assertEquals(UpdateTrack.PRIVATE, Distribute.getUpdateTrack());
    }
}
