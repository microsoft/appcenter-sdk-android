package com.microsoft.azure.mobile.sasquatch.activities;


import android.support.annotation.StringRes;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.sasquatch.R;

import org.hamcrest.Matcher;
import org.junit.Assert;
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

    private static final int CHECK_DELAY = 50;
    private static final int TOAST_DELAY = 2000;

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

        /* Check toasts. */
        waitFor(onToast(withText(R.string.event_before_sending)), Constants.DEFAULT_TRIGGER_INTERVAL + CHECK_DELAY)
                .check(matches(isDisplayed()));
        waitAnalytics();
        waitFor(onToast(anyOf(withContainsText(R.string.event_sent_succeeded), withContainsText(R.string.event_sent_failed))), TOAST_DELAY)
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

        /* Check toasts. */
        waitFor(onToast(withText(R.string.page_before_sending)), Constants.DEFAULT_TRIGGER_INTERVAL + CHECK_DELAY)
                .check(matches(isDisplayed()));
        waitAnalytics();
        waitFor(onToast(anyOf(withContainsText(R.string.page_sent_succeeded), withContainsText(R.string.page_sent_failed))), TOAST_DELAY)
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
        onView(isRoot()).perform(waitFor(CHECK_DELAY));
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

    private static ViewInteraction waitFor(final ViewInteraction viewInteraction, final long millis) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + millis;
        final View[] found = new View[] { null };
        while (System.currentTimeMillis() < endTime)
        {
            viewInteraction.check(new ViewAssertion() {

                @Override
                public void check(View view, NoMatchingViewException noViewFoundException) {
                    found[0] = view;
                }
            });
            if (found[0] != null)
                return viewInteraction;
            Thread.sleep(CHECK_DELAY);
        }
        Assert.fail();
        return viewInteraction;
    }
}
