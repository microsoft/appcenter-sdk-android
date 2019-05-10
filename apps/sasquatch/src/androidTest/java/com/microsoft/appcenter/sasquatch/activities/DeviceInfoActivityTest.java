/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;


import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class DeviceInfoActivityTest {

    @Rule
    public ActivityTestRule<DeviceInfoActivity> mActivityTestRule = new ActivityTestRule<>(DeviceInfoActivity.class);

    @Test
    public void allValuesExist() {
        onInfo("SdkName").check(matches(anything()));
        onInfo("SdkVersion").check(matches(anything()));
        onInfo("Model").check(matches(anything()));
        onInfo("OemName").check(matches(anything()));
        onInfo("OsName").check(matches(anything()));
        onInfo("OsVersion").check(matches(anything()));
        onInfo("OsBuild").check(matches(anything()));
        onInfo("OsApiLevel").check(matches(anything()));
        onInfo("Locale").check(matches(anything()));
        onInfo("TimeZoneOffset").check(matches(anything()));
        onInfo("ScreenSize").check(matches(anything()));
        onInfo("AppVersion").check(matches(anything()));
        onInfo("CarrierName").check(matches(anything()));
        onInfo("CarrierCountry").check(matches(anything()));
        onInfo("AppBuild").check(matches(anything()));
        onInfo("AppNamespace").check(matches(anything()));
    }

    private DataInteraction onInfo(String title) {
        return onData(allOf(instanceOf(DeviceInfoActivity.DeviceInfoDisplayModel.class), withInfoTitle(title)));
    }

    private static Matcher<Object> withInfoTitle(String expectedTitle) {
        assertNotNull(expectedTitle);
        return withInfoTitle(equalTo(expectedTitle));
    }

    private static Matcher<Object> withInfoTitle(final Matcher<String> itemTitleMatcher) {
        assertNotNull(itemTitleMatcher);
        return new BoundedMatcher<Object, DeviceInfoActivity.DeviceInfoDisplayModel>(DeviceInfoActivity.DeviceInfoDisplayModel.class) {
            @Override
            public boolean matchesSafely(DeviceInfoActivity.DeviceInfoDisplayModel map) {
                return itemTitleMatcher.matches(map.title);
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("with item title: ");
                itemTitleMatcher.describeTo(description);
            }
        };
    }
}
