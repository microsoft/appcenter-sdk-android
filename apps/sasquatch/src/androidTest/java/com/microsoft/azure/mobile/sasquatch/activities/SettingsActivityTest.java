package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.DataInteraction;
import android.support.test.rule.ActivityTestRule;

import com.microsoft.azure.mobile.sasquatch.R;

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

public class SettingsActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getTargetContext();

        /* Clear preferences. */
        mContext.getSharedPreferences("MobileCenter", Context.MODE_PRIVATE).edit().clear().apply();

        /* Launch activity. */
        mActivityRule.launchActivity(new Intent());
        onView(withId(R.id.action_settings)).perform(click());
    }

    @Test
    public void testInitialState() {
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
    }

    @Test
    public void testEnableMobileCenter() {

        /* Disable mobile center. */
        onCheckbox(R.string.mobile_center_state_key).perform(click());

        /* Check services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isNotChecked()));
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isNotChecked()));
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isNotChecked()));
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isNotChecked()));

        /* Enable mobile center. */
        onCheckbox(R.string.mobile_center_state_key).perform(click());

        /* Check services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
    }

    @Test
    public void testEnableAnalytics() {

        /* Disable mobile center. */
        onCheckbox(R.string.mobile_center_analytics_state_key).perform(click());

        /* Check services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isNotChecked()));
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));

        /* Enable mobile center. */
        onCheckbox(R.string.mobile_center_analytics_state_key).perform(click());

        /* Check services state. */
        onCheckbox(R.string.mobile_center_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_analytics_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_crashes_state_key).check(matches(isChecked()));
        onCheckbox(R.string.mobile_center_distribute_state_key).check(matches(isChecked()));
    }

    private DataInteraction onCheckbox(int id) {
        return onData(withKey(mContext.getString(id))).onChildView(withId(android.R.id.checkbox));
    }
}