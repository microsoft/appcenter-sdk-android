/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.CrashTestHelper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.microsoft.appcenter.sasquatch.activities.CrashSubActivity.INTENT_EXTRA_CRASH_TYPE;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class CrashActivity extends AppCompatActivity {

    private boolean mCrashSuperPauseNotCalled;

    private boolean mCrashSuperDestroyNotCalled;

    private final List<CrashTestHelper.Crash> mSpecificCrashes = Arrays.asList(
            new CrashTestHelper.Crash(R.string.title_memory_crash2, R.string.description_memory_crash2, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(CrashSubActivity.INTENT_EXTRA_CRASH_TYPE, 1));
                }
            }),
            new CrashTestHelper.Crash(R.string.title_variable_message2, R.string.description_variable_message2, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(INTENT_EXTRA_CRASH_TYPE, 2));
                }
            }),
            new CrashTestHelper.Crash(R.string.title_test_ui_crash, R.string.description_test_ui_crash, new Runnable() {

                @Override
                public void run() {
                    ListView view = findViewById(R.id.list);
                    view.setAdapter(new ArrayAdapter<>(CrashActivity.this, android.R.layout.simple_list_item_2, mSpecificCrashes));
                }
            }),
            new CrashTestHelper.Crash(R.string.title_super_not_called_exception, R.string.description_super_not_called_exception, new Runnable() {

                @Override
                public void run() {
                    mCrashSuperPauseNotCalled = true;
                    finish();
                }
            }),
            new CrashTestHelper.Crash(R.string.title_super_not_called_exception2, R.string.description_super_not_called_exception2, new Runnable() {

                @Override
                public void run() {
                    mCrashSuperDestroyNotCalled = true;
                    finish();
                }
            }),
            new CrashTestHelper.Crash(R.string.title_super_not_called_exception3, R.string.description_super_not_called_exception3, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity.class).putExtra(INTENT_EXTRA_CRASH_TYPE, 0));
                }
            }),
            new CrashTestHelper.Crash(R.string.title_super_not_called_exception4, R.string.description_super_not_called_exception4, new Runnable() {

                @Override
                public void run() {
                    startActivity(new Intent(CrashActivity.this, CrashSubActivity2.class));
                }
            }),

            /* NDK crashes */
            new CrashTestHelper.Crash(R.string.title_test_native_crash, R.string.description_test_native_crash, new Runnable() {

                @Override
                public void run() {
                    nativeDereferenceNullPointer();
                }
            }),
            new CrashTestHelper.Crash(R.string.title_native_stack_overflow_crash, R.string.description_native_stack_overflow_crash, new Runnable() {

                @Override
                public void run() {
                    nativeStackOverflowCrash();
                }
            }),
            new CrashTestHelper.Crash(R.string.title_native_abort_crash, R.string.description_native_abort_crash, new Runnable() {

                @Override
                public void run() {
                    nativeAbortCall();
                }
            }),
            new CrashTestHelper.Crash(R.string.title_low_memory_warning, R.string.description_low_memory_warning, new Runnable() {

                @Override
                public void run() {
                    final AtomicInteger i = new AtomicInteger(0);
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            nativeAllocateLargeBuffer();
                            Log.d(LOG_TAG, "Memory allocated: " + i.addAndGet(128) + "MB");
                            handler.post(this);
                        }
                    });
                }
            })
    );

    private native void nativeAllocateLargeBuffer();

    private native void nativeDereferenceNullPointer();

    private native void nativeStackOverflowCrash();

    private native void nativeAbortCall();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        ListView listView = findViewById(R.id.list);
        ArrayList<CrashTestHelper.Crash> crashes = new ArrayList<>(new CrashTestHelper(this).getCrashes());
        crashes.addAll(mSpecificCrashes);
        listView.setAdapter(new ArrayAdapter<CrashTestHelper.Crash>(this, android.R.layout.simple_list_item_2, android.R.id.text1, crashes) {

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
                ((CrashTestHelper.Crash) parent.getItemAtPosition(position)).crashTask.run();
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
}
