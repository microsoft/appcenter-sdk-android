/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.sasquatch.CrashTestHelper;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.AttachmentsUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class ManagedErrorActivity extends PropertyActivity {

    private Spinner mHandledErrorsSpinner;

    private List<CrashTestHelper.Crash> mCrashes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.layout_handled_error, ((LinearLayout) findViewById(R.id.top_layout)));
        mCrashes = new CrashTestHelper(this).getCrashes();

        /* Handled Errors Spinner. */
        mHandledErrorsSpinner = findViewById(R.id.handled_errors_spinner);
        mHandledErrorsSpinner.setAdapter(new ArrayAdapter<CrashTestHelper.Crash>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mCrashes) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return setTextView(position, super.getView(position, convertView, parent));
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return setTextView(position, super.getDropDownView(position, convertView, parent));
            }

            @NonNull
            @SuppressWarnings("ConstantConditions")
            private View setTextView(int position, View view) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).title);
                return view;
            }
        });
    }

    @Override
    protected void send(View view) {
        try {
            mCrashes.get(mHandledErrorsSpinner.getSelectedItemPosition()).crashTask.run();
        } catch (Throwable t) {
            Map<String, String> properties = readStringProperties();
            Iterable<ErrorAttachmentLog> attachmentLogs = AttachmentsUtil.getInstance().getErrorAttachments(getApplicationContext());
            Crashes.trackError(t, properties, attachmentLogs);
        }
    }

    @Override
    protected boolean isStringTypeOnly() {
        return true;
    }
}