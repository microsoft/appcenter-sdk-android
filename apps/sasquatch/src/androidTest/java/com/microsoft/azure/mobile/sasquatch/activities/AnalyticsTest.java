package com.microsoft.azure.mobile.sasquatch.activities;


import android.support.annotation.StringRes;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.sasquatch.R;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@SuppressWarnings("unused")
public class AnalyticsTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void sendEventTest() throws InterruptedException {

        /* Send event. */
        onView(allOf(
                withChild(withText(R.string.title_event)),
                withChild(withText(R.string.description_event))))
                .perform(click());
        onView(withId(R.id.name)).perform(replaceText("test"), closeSoftKeyboard());
        onView(withText(R.string.send)).perform(click());

        /* Wait trigger interval. */
        Thread.sleep(Constants.DEFAULT_TRIGGER_INTERVAL);

        /* Check toast. */
        /* TODO Unstable check: interval may be started via start session or event will be delivered very fast. */
        //onToast(withText(R.string.event_before_sending))
        //        .check(matches(isDisplayed()));

        /* Wait for sending result. */
        waitAnalytics();

        /* Check toast. */
        onToast(anyOf(withContainsText(R.string.event_sent_succeeded), withContainsText(R.string.event_sent_failed)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void sendPageTest() throws InterruptedException {

        /* Send page. */
        onView(allOf(
                withChild(withText(R.string.title_page)),
                withChild(withText(R.string.description_page))))
                .perform(click());
        onView(withId(R.id.name)).perform(replaceText("test"), closeSoftKeyboard());
        onView(withText(R.string.send)).perform(click());

        /* Wait trigger interval. */
        Thread.sleep(Constants.DEFAULT_TRIGGER_INTERVAL);

        /* Check toast. */
        /* TODO Unstable check: interval may be started via start session or page will be delivered very fast. */
        //onToast(withText(R.string.page_before_sending))
        //        .check(matches(isDisplayed()));

        /* Wait for sending result. */
        waitAnalytics();

        /* Check toast. */
        onToast(anyOf(withContainsText(R.string.page_sent_succeeded), withContainsText(R.string.page_sent_failed)))
                .check(matches(isDisplayed()));
    }

    private ViewInteraction onToast(final Matcher<View> viewMatcher) {
        return onView(viewMatcher).inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))));
    }

    private Matcher<View> withContainsText(@StringRes final int resourceId) {
        return withText(containsString(mActivityTestRule.getActivity().getString(resourceId)));
    }

    private void waitAnalytics() {
        Espresso.registerIdlingResources(MainActivity.analyticsIdlingResource);
        onView(isRoot()).perform(waitFor(100));
        Espresso.unregisterIdlingResources(MainActivity.analyticsIdlingResource);
    }

    private static ViewAction waitFor(final long millis) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
