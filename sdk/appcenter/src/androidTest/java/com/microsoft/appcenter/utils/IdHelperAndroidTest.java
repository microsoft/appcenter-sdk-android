/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;


@SuppressWarnings("unused")
public class IdHelperAndroidTest {

    private static final String TAG = "IdHelper";

    @BeforeClass
    public static void setUpClass() {
        SharedPreferencesManager.initialize(InstrumentationRegistry.getTargetContext());
    }


    @Test
    public void getInstallId() {
        Log.i(TAG, "Testing installId-shortcut");
        UUID expected = UUID.randomUUID();
        SharedPreferencesManager.putString(PrefStorageConstants.KEY_INSTALL_ID, expected.toString());

        UUID actual = IdHelper.getInstallId();
        assertEquals(expected, actual);

        String wrongUUID = "1234567";
        SharedPreferencesManager.putString(PrefStorageConstants.KEY_INSTALL_ID, expected.toString());

        actual = IdHelper.getInstallId();
        assertNotEquals(wrongUUID, actual);
        assertNotNull(actual);
    }
}