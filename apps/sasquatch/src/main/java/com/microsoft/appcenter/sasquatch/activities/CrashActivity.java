/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.TestCrashException;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.microsoft.appcenter.sasquatch.activities.CrashSubActivity.INTENT_EXTRA_CRASH_TYPE;

public class CrashActivity extends AppCompatActivity {

    private boolean mCrashSuperPauseNotCalled;

    private boolean mCrashSuperDestroyNotCalled;

    private final List<Crash> sCrashes = Arrays.asList(
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
            new Crash(R.string.title_test_ui_crash, R.string.description_test_ui_crash, new Runnable() {

                @Override
                public void run() {
                    ListView view = findViewById(R.id.list);
                    view.setAdapter(new ArrayAdapter<>(CrashActivity.this, android.R.layout.simple_list_item_2, sCrashes));
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
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(INTENT_EXTRA_CRASH_TYPE, 1));
                }
            }),
            new Crash(R.string.title_variable_message, R.string.description_variable_message, new Runnable() {

                @Override
                public void run() {
                    getResources().openRawResource(~new Random().nextInt(10));
                }
            }),
            new Crash(R.string.title_variable_message2, R.string.description_variable_message2, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(INTENT_EXTRA_CRASH_TYPE, 2));
                }
            }),
            new Crash(R.string.title_super_not_called_exception, R.string.description_super_not_called_exception, new Runnable() {

                @Override
                public void run() {
                    mCrashSuperPauseNotCalled = true;
                    finish();
                }
            }),
            new Crash(R.string.title_super_not_called_exception2, R.string.description_super_not_called_exception2, new Runnable() {

                @Override
                public void run() {
                    mCrashSuperDestroyNotCalled = true;
                    finish();
                }
            }),
            new Crash(R.string.title_super_not_called_exception3, R.string.description_super_not_called_exception3, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(INTENT_EXTRA_CRASH_TYPE, 0));
                }
            }),
            new Crash(R.string.title_super_not_called_exception4, R.string.description_super_not_called_exception4, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity2.class));
                }
            }),

            /* NDK crashes */
            new Crash(R.string.title_test_native_crash, R.string.description_test_native_crash, new Runnable() {

                @Override
                public void run() {
                    nativeDereferenceNullPointer();
                }
            }),
            new Crash(R.string.title_native_stack_overflow_crash, R.string.description_native_stack_overflow_crash, new Runnable() {

                @Override
                public void run() {
                    nativeStackOverflowCrash();
                }
            }),
            new Crash(R.string.title_native_abort_crash, R.string.description_native_abort_crash, new Runnable() {

                @Override
                public void run() {
                    nativeAbortCall();
                }
            })
    );

    private native void nativeDereferenceNullPointer();

    private native void nativeStackOverflowCrash();

    private native void nativeAbortCall();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new ArrayAdapter<Crash>(this, android.R.layout.simple_list_item_2, android.R.id.text1, sCrashes) {

            @SuppressWarnings("ConstantConditions")
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).title);
                ((TextView) view.findViewById(android.R.id.text2)).setText(getItem(position).description);
                return view;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((Crash) parent.getItemAtPosition(position)).crashTask.run();
            }
        });
    }

    @Override
    protected void onPause() {
        if (!mCrashSuperPauseNotCalled) {
            super.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (!mCrashSuperDestroyNotCalled) {
            super.onDestroy();
        }
    }

    @VisibleForTesting
    static class Crash {

        final int title;

        final int description;

        final Runnable crashTask;

        Crash(int title, int description, Runnable crashTask) {
            this.title = title;
            this.description = description;
            this.crashTask = crashTask;
        }
    }
}
