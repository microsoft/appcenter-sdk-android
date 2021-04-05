/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities.utils;

import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public final class EspressoUtils {

    public static final int CHECK_DELAY = 50;
    public static final int TOAST_DELAY = 2000;

    private EspressoUtils() {
    }

    public static ViewInteraction onToast(Activity activity, final Matcher<View> viewMatcher) {
        return onView(viewMatcher).inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))));
    }

    public static Matcher<View> withContainsText(@StringRes final int resourceId) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with text from resource id: ");
                description.appendValue(resourceId);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                String substring = textView.getResources().getString(resourceId);
                String text = textView.getText().toString();
                return text.contains(substring);
            }
        };
    }

    public static ViewAction waitFor(final long millis) {
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

    public static ViewInteraction waitFor(final ViewInteraction viewInteraction, final long millis) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + millis;
        final View[] found = new View[] { null };
        while (System.currentTimeMillis() < endTime)
        {
            try {
                viewInteraction.check(new ViewAssertion() {

                    @Override
                    public void check(View view, NoMatchingViewException noViewFoundException) {
                        found[0] = view;
                    }
                });
            } catch (NoMatchingRootException ignored) {
            }
            if (found[0] != null)
                return viewInteraction;
            Thread.sleep(CHECK_DELAY);
        }
        Assert.fail();
        return viewInteraction;
    }
}
