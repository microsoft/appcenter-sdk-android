package com.microsoft.azure.mobile.sasquatch.activities;


import android.support.test.rule.ActivityTestRule;

import com.microsoft.azure.mobile.sasquatch.R;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@SuppressWarnings("unused")
public class AnalyticsTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void sendEventTest() {
        onView(allOf(
                withChild(withText(R.string.title_event)),
                withChild(withText(R.string.description_event))))
                .perform(click());
        onView(withId(R.id.name)).perform(replaceText("test"), closeSoftKeyboard());
        onView(withText(R.string.send)).perform(click());
        onView(withText(R.string.description_event))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void sendPageTest() {
        onView(allOf(
                withChild(withText(R.string.title_page)),
                withChild(withText(R.string.description_page))))
                .perform(click());
        onView(withId(R.id.name)).perform(replaceText("test"), closeSoftKeyboard());
        onView(withText(R.string.send)).perform(click());
        onView(withText(R.string.description_page))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }
}
