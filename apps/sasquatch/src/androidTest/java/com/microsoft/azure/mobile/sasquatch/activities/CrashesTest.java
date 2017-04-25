package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.EspressoException;
import android.support.test.espresso.FailureHandler;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.IntentCompat;
import android.view.View;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.CrashesPrivateHelper;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.microsoft.azure.mobile.sasquatch.activities.utils.EspressoUtils.CHECK_DELAY;
import static com.microsoft.azure.mobile.sasquatch.activities.utils.EspressoUtils.TOAST_DELAY;
import static com.microsoft.azure.mobile.sasquatch.activities.utils.EspressoUtils.onToast;
import static com.microsoft.azure.mobile.sasquatch.activities.utils.EspressoUtils.waitFor;
import static com.microsoft.azure.mobile.sasquatch.activities.utils.EspressoUtils.withContainsText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class CrashesTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getTargetContext();

        /* Clear preferences. */
        StorageHelper.initialize(mContext);
        StorageHelper.PreferencesStorage.clear();

        /* Clear crashes. */
        Constants.loadFromContext(mContext);
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }

        /* Launch main activity and go to setting page. Required to properly initialize. */
        mActivityTestRule.launchActivity(new Intent());

        /* Register IdlingResource */
        Espresso.registerIdlingResources(MainActivity.crashesIdlingResource);
    }

    @After
    public final void tearDown() {

        /* Unregister IdlingResource */
        Espresso.unregisterIdlingResources(MainActivity.crashesIdlingResource);
    }

    @Test
    public void testCrashTest() throws InterruptedException {
        crashTest(R.string.title_test_crash);
    }

    @Test
    public void divideByZeroTest() throws InterruptedException {
        crashTest(R.string.title_crash_divide_by_0);
    }

    @Test
    public void uiCrashTest() throws InterruptedException {
        crashTest(R.string.title_test_ui_crash);
    }

    @Test
    public void variableMessageTest() throws InterruptedException {
        crashTest(R.string.title_variable_message);
    }

    /**
     * Crash and sending report test.
     * <p>
     * We can't truly restart application in tests, so some kind of crashes can't be tested by this method.
     * Out of memory or stack overflow - crash the test process;
     * UI states errors - problems with restart activity;
     * <p>
     * Also to avoid flakiness, please setup your test environment
     * (https://google.github.io/android-testing-support-library/docs/espresso/setup/index.html#setup-your-test-environment).
     * On your device, under Settings->Developer options disable the following 3 settings:
     * - Window animation scale
     * - Transition animation scale
     * - Animator duration scale
     *
     * @param titleId Title string resource to find list item.
     * @throws InterruptedException If the current thread is interrupted.
     */
    private void crashTest(@StringRes int titleId) throws InterruptedException {

        /* Crash. */
        onView(allOf(
                withChild(withText(R.string.title_crashes)),
                withChild(withText(R.string.description_crashes))))
                .perform(click());

        onCrash(titleId)
                .withFailureHandler(new CrashFailureHandler())
                .perform(click());

        /* Check error report. */
        assertTrue(Crashes.hasCrashedInLastSession());

        /* Send report. */
        waitFor(onView(withText(R.string.crash_confirmation_dialog_send_button))
                .inRoot(isDialog()), 1000)
                .perform(click());

        /* Check toasts. */
        waitFor(onToast(mActivityTestRule.getActivity(),
                withText(R.string.crash_before_sending)), CHECK_DELAY)
                .check(matches(isDisplayed()));
        onView(isRoot()).perform(waitFor(CHECK_DELAY));
        waitFor(onToast(mActivityTestRule.getActivity(), anyOf(
                withContainsText(R.string.crash_sent_succeeded),
                withText(R.string.crash_sent_failed))), TOAST_DELAY)
                .check(matches(isDisplayed()));
        onView(isRoot()).perform(waitFor(TOAST_DELAY));
    }

    private ViewInteraction onCrash(@StringRes int titleId) {
        return onData(allOf(instanceOf(CrashActivity.Crash.class), withCrashTitle(titleId)))
                .perform();
    }

    @SuppressWarnings("rawtypes")
    private static Matcher<Object> withCrashTitle(@StringRes final int titleId) {
        return new BoundedMatcher<Object, CrashActivity.Crash>(CrashActivity.Crash.class) {

            @Override
            public boolean matchesSafely(CrashActivity.Crash map) {
                return map.title == titleId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with item title from resource id: ");
                description.appendValue(titleId);
            }
        };
    }

    private class CrashFailureHandler implements FailureHandler {

        @Override
        public void handle(Throwable error, Matcher<View> viewMatcher) {
            Throwable uncaughtException = error instanceof EspressoException ? error.getCause() : error;

            /* Save exception. */
            CrashesPrivateHelper.saveUncaughtException(mContext.getMainLooper().getThread(), uncaughtException);

            /* Relaunch. */
            ActivityCompat.finishAffinity(mActivityTestRule.getActivity());
            unsetInstance(MobileCenter.class);
            unsetInstance(Crashes.class);
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            mActivityTestRule.launchActivity(intent);
        }

        @SuppressWarnings("unchecked")
        private void unsetInstance(Class clazz) {
            try {
                Method m = clazz.getDeclaredMethod("unsetInstance");
                m.setAccessible(true);
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}