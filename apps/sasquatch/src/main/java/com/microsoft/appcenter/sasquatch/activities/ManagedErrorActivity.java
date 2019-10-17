/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class ManagedErrorActivity extends PropertyActivity {

    private static final List<Class<? extends Throwable>> sSupportedThrowables = Arrays.asList(
            ArithmeticException.class,
            ArrayIndexOutOfBoundsException.class,
            ArrayStoreException.class,
            ClassCastException.class,
            ClassNotFoundException.class,
            CloneNotSupportedException.class,
            IllegalAccessException.class,
            IllegalArgumentException.class,
            IllegalMonitorStateException.class,
            IllegalStateException.class,
            IllegalThreadStateException.class,
            IndexOutOfBoundsException.class,
            InstantiationException.class,
            InterruptedException.class,
            NegativeArraySizeException.class,
            NoSuchFieldException.class,
            NoSuchMethodException.class,
            NullPointerException.class,
            NumberFormatException.class,
            SecurityException.class,
            UnsupportedOperationException.class,
            AssertionError.class,
            LinkageError.class,
            ThreadDeath.class,
            InternalError.class,
            OutOfMemoryError.class);

    private Spinner mHandledErrorsSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        @SuppressLint("InflateParams") View middleView = getLayoutInflater().inflate(R.layout.layout_handled_error, null);
        ((LinearLayout) findViewById(R.id.middle_layout)).addView(middleView);

        /* Handled Errors Spinner. */
        mHandledErrorsSpinner = findViewById(R.id.handled_errors_spinner);
        mHandledErrorsSpinner.setAdapter(new ArrayAdapter<Class<? extends Throwable>>(this, android.R.layout.simple_list_item_1, sSupportedThrowables) {

            @SuppressWarnings("ConstantConditions")
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).getSimpleName());
                return view;
            }
        });
    }

    @Override
    protected void send(View view) {
        try {
            Throwable throwable = sSupportedThrowables.get(mHandledErrorsSpinner.getSelectedItemPosition()).newInstance();
            Map<String, String> properties = readStringProperties();
            Crashes.class.getMethod("trackException", Throwable.class, Map.class).invoke(null, throwable, properties);

            /* TODO uncomment the next line, remove reflection and catch block after API available to jCenter. */
            /* Crashes.trackException(throwable, properties); */
        } catch (Exception e) {
            //noinspection ConstantConditions
            Log.d(LOG_TAG, e.getMessage());
        }
    }

    @Override
    protected boolean isStringTypeOnly() {
        return true;
    }
}