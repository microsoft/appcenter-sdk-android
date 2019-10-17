/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch;

import android.app.Activity;
import android.content.Intent;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.TestCrashException;
import com.microsoft.appcenter.sasquatch.activities.CrashSubActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CrashTestHelper {

    private Activity mActivity;
    private final List<Crash> mCrashes = Arrays.asList(
            new Crash(R.string.title_test_crash, R.string.description_test_crash, new Runnable() {

                @Override
                public void run() {
                    Crashes.generateTestCrash();
                    throw new TestCrashException();
                }
            }),
            new Crash(R.string.title_crash_divide_by_0, R.string.description_crash_divide_by_0, new Runnable() {

                @Override
                @SuppressWarnings("ResultOfMethodCallIgnored")
                public void run() {
                    ("" + (42 / Integer.valueOf("0"))).toCharArray();
                }
            }),
            new Crash(R.string.title_stack_overflow_crash, R.string.description_stack_overflow_crash, new Runnable() {

                @Override
                @SuppressWarnings("InfiniteRecursion")
                public void run() {
                    run();
                }
            }),
            new Crash(R.string.title_deeply_nested_exception_crash, R.string.description_deeply_nested_exception_crash, new Runnable() {

                @Override
                public void run() {
                    Exception e = new Exception();
                    for (int i = 0; i < 1000; i++) {
                        e = new Exception(String.valueOf(i), e);
                    }
                    throw new RuntimeException(e);
                }
            }),
            new Crash(R.string.title_memory_crash, R.string.description_memory_crash, new Runnable() {

                @Override
                public void run() {
                    new int[Integer.MAX_VALUE].clone();
                }
            }),
            new Crash(R.string.title_memory_crash2, R.string.description_memory_crash2, new Runnable() {

                @Override
                public void run() {
                    mActivity.startActivity(new Intent(mActivity, CrashSubActivity.class).putExtra(CrashSubActivity.INTENT_EXTRA_CRASH_TYPE, 1));
                }
            }),
            new Crash(R.string.title_variable_message, R.string.description_variable_message, new Runnable() {

                @Override
                public void run() {
                    mActivity.getResources().openRawResource(~new Random().nextInt(10));
                }
            })
    );

    public CrashTestHelper(Activity activity) {
        mActivity = activity;
    }

    public List<Crash> getCrashes() {
        return mCrashes;
    }

    public static class Crash {

        public final int title;

        public final int description;

        public final Runnable crashTask;

        public Crash(int title, int description, Runnable crashTask) {
            this.title = title;
            this.description = description;
            this.crashTask = crashTask;
        }
    }
}
