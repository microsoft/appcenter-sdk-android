package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.DataInteraction;
import android.support.test.rule.ActivityTestRule;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@SuppressWarnings("unused")
public class SettingsActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getTargetContext();

        /* Clear preferences. */
        StorageHelper.initialize(mContext);
        StorageHelper.PreferencesStorage.clear();

        /* Launch main activity and go to setting page. Required to properly initialize. */
        mActivityTestRule.launchActivity(new Intent());
        onView(withId(R.id.action_settings)).perform(click());
    }

    @Test
    public void testInitialState() {
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    @Test
    public void testEnableMobileCenter() {

        /* Disable mobile center. */
        onCheckbox(R.string.mobile_center_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isPushEnabled());

        /* Unable enable analytics when mobile center is disabled. */
        onCheckbox(R.string.mobile_center_analytics_state_key).perform(click());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isAnalyticsEnabled());

        /* Unable enable crashes when mobile center is disabled. */
        onCheckbox(R.string.mobile_center_crashes_state_key).perform(click());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isCrashesEnabled());

        /* Unable enable distribute when mobile center is disabled. */
        onCheckbox(R.string.mobile_center_distribute_state_key).perform(click());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isDistributeEnabled());

        /* Unable enable push when mobile center is disabled. */
        onCheckbox(R.string.mobile_center_push_state_key).perform(click());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isPushEnabled());

        /* Enable mobile center. */
        onCheckbox(R.string.mobile_center_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    @Test
    public void testEnableAnalytics() {

        /* Disable analytics service. */
        onCheckbox(R.string.mobile_center_analytics_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());

        /* Enable analytics service. */
        onCheckbox(R.string.mobile_center_analytics_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    @Test
    public void testEnableCrashes() {

        /* Disable distribute service. */
        onCheckbox(R.string.mobile_center_crashes_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());

        /* Enable distribute service. */
        onCheckbox(R.string.mobile_center_crashes_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    @Test
    public void testEnableDistribute() {

        /* Disable distribute service. */
        onCheckbox(R.string.mobile_center_distribute_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());

        /* Enable distribute service. */
        onCheckbox(R.string.mobile_center_distribute_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    @Test
    public void testEnablePush() {

        /* Disable push service. */
        onCheckbox(R.string.mobile_center_push_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isNotChecked()));
        Assert.assertFalse(GetHelper.isPushEnabled());

        /* Enable push service. */
        onCheckbox(R.string.mobile_center_push_state_key).perform(click());

        /* Check mobile center and services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCoreEnabled());
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isAnalyticsEnabled());
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isCrashesEnabled());
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isDistributeEnabled());
        onCheckbox(R.string.mobile_center_push_state_key).check(matches(isChecked()));
        Assert.assertTrue(GetHelper.isPushEnabled());
    }

    private DataInteraction onCheckbox(int id) {
        return onData(withKey(mContext.getString(id))).onChildView(withId(android.R.id.checkbox));
    }
}