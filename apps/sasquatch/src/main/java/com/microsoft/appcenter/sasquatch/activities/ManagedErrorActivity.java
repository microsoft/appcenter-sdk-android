/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.appcenter.crashes.CrashesPrivateHelper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ManagedErrorActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new ArrayAdapter<Class<? extends Throwable>>(this, android.R.layout.simple_list_item_1, sSupportedThrowables) {
            @SuppressWarnings("ConstantConditions")
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).getSimpleName());
                return view;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Throwable> clazz = (Class<? extends Throwable>) parent.getItemAtPosition(position);
                    Throwable e;
                    try {
                        e = clazz.getConstructor(String.class).newInstance("Test Exception");
                    } catch (NoSuchMethodException ignored) {
                        e = clazz.getConstructor().newInstance();
                    }
                    CrashesPrivateHelper.trackException(e,
                            new HashMap<String, String>() {{
                                put("prop1", "value1");
                                put("prop2", "value2");
                            }});
                } catch (Exception e) {

                    /* This is not expected behavior so let the application crashes. */
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
