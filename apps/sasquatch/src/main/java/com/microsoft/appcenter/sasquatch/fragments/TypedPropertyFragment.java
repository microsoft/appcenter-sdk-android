/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.microsoft.appcenter.analytics.EventProperties;
import com.microsoft.appcenter.analytics.PropertyConfigurator;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Date;
import java.util.Map;

public class TypedPropertyFragment extends EditDateTimeFragment {

    private EditText mEditKey;

    private Spinner mEditType;

    private EditText mEditString;

    private EditText mEditNumberDouble;

    private EditText mEditNumberLong;

    private CheckBox mEditBool;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.typed_property, container, false);

        /* Find views. */
        mEditKey = view.findViewById(R.id.key);
        mEditType = view.findViewById(R.id.type);
        mEditString = view.findViewById(R.id.string);
        mEditNumberDouble = view.findViewById(R.id.number_double);
        mEditNumberLong = view.findViewById(R.id.number_long);
        mEditBool = view.findViewById(R.id.bool);

        /* Set change type callback. */
        mEditType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateValueType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        resetValue();
        return view;
    }

    private void updateValueType() {
        EventPropertyType type = getType();
        mEditString.setVisibility(type == EventPropertyType.STRING ? View.VISIBLE : View.GONE);
        mEditBool.setVisibility(type == EventPropertyType.BOOLEAN ? View.VISIBLE : View.GONE);
        mEditNumberDouble.setVisibility(type == EventPropertyType.NUMBER_DOUBLE ? View.VISIBLE : View.GONE);
        mEditNumberLong.setVisibility(type == EventPropertyType.NUMBER_LONG ? View.VISIBLE : View.GONE);
        mDateTime.setVisibility(type == EventPropertyType.DATETIME ? View.VISIBLE : View.GONE);
        resetValue();
    }

    public EventPropertyType getType() {
        return EventPropertyType.values()[mEditType.getSelectedItemPosition()];
    }

    public void set(Map<String, String> eventProperties) {
        EventPropertyType type = getType();
        String key = mEditKey.getText().toString();
        if (type == EventPropertyType.STRING) {
            eventProperties.put(key, mEditString.getText().toString());
        }
    }

    public void set(EventProperties eventProperties) {
        EventPropertyType type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case BOOLEAN:
                eventProperties.set(key, mEditBool.isChecked());
                break;
            case NUMBER_DOUBLE: {
                String stringValue = mEditNumberDouble.getText().toString();
                if (!stringValue.isEmpty()) {
                    double value = Double.parseDouble(stringValue);
                    eventProperties.set(key, value);
                } else {
                    eventProperties.set(key, Double.NaN);
                }
                break;
            }
            case NUMBER_LONG: {
                String stringValue = mEditNumberLong.getText().toString();
                if (!stringValue.isEmpty()) {
                    long value = Long.parseLong(stringValue);
                    eventProperties.set(key, value);
                }
                break;
            }
            case DATETIME:
                eventProperties.set(key, mDate);
                break;
            case STRING:
                eventProperties.set(key, mEditString.getText().toString());
                break;
        }
    }

    public void set(PropertyConfigurator configurator) {
        EventPropertyType type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case BOOLEAN:
                configurator.setEventProperty(key, mEditBool.isChecked());
                break;
            case NUMBER_DOUBLE: {
                String stringValue = mEditNumberDouble.getText().toString();
                if (!stringValue.isEmpty()) {
                    double value = Double.parseDouble(stringValue);
                    configurator.setEventProperty(key, value);
                } else {
                    configurator.setEventProperty(key, Double.NaN);
                }
                break;
            }
            case NUMBER_LONG: {
                String stringValue = mEditNumberLong.getText().toString();
                if (!stringValue.isEmpty()) {
                    long value = Long.parseLong(stringValue);
                    configurator.setEventProperty(key, value);
                }
                break;
            }
            case DATETIME:
                configurator.setEventProperty(key, mDate);
                break;
            case STRING:
                configurator.setEventProperty(key, mEditString.getText().toString());
                break;
        }
    }

    public void setGenericProperty(Map<String, Object> map) {
        EventPropertyType type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case BOOLEAN:
                map.put(key, mEditBool.isChecked());
                break;
            case NUMBER_DOUBLE: {
                String stringValue = mEditNumberDouble.getText().toString();
                if (!stringValue.isEmpty()) {
                    double value = Double.parseDouble(stringValue);
                    map.put(key, value);
                } else {
                    map.put(key, Double.NaN);
                }
                break;
            }
            case NUMBER_LONG: {
                String stringValue = mEditNumberLong.getText().toString();
                if (!stringValue.isEmpty()) {
                    long value = Long.parseLong(stringValue);
                    map.put(key, value);
                }
                break;
            }
            case DATETIME:
                map.put(key, mDate);
                break;
            case STRING:
                map.put(key, mEditString.getText().toString());
                break;
        }
    }

    public void reset() {
        resetValue();
        mEditType.setSelection(0);
    }

    private void resetValue() {
        mEditString.setText("");
        mEditBool.setChecked(false);
        mEditNumberDouble.setText("0");
        mEditNumberLong.setText("0");
        setDate(new Date());
    }

    public enum EventPropertyType {
        STRING,
        BOOLEAN,
        NUMBER_DOUBLE,
        NUMBER_LONG,
        DATETIME
    }
}
