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

import com.microsoft.appcenter.CustomProperties;
import com.microsoft.appcenter.sasquatch.R;

public class CustomPropertyFragment extends EditDateTimeFragment {

    private EditText mEditKey;

    private Spinner mEditType;

    private EditText mEditString;

    private EditText mEditNumber;

    private CheckBox mEditBool;

    private View mValueLabel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.custom_property, container, false);

        /* Find views. */
        mEditKey = view.findViewById(R.id.key);
        mEditType = view.findViewById(R.id.type);
        mEditString = view.findViewById(R.id.string);
        mEditNumber = view.findViewById(R.id.number);
        mEditBool = view.findViewById(R.id.bool);
        mValueLabel = view.findViewById(R.id.value_label);

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
        return view;
    }

    private void updateValueType() {
        CustomPropertyType type = getType();
        mEditString.setVisibility(type == CustomPropertyType.STRING ? View.VISIBLE : View.GONE);
        mEditNumber.setVisibility(type == CustomPropertyType.NUMBER ? View.VISIBLE : View.GONE);
        mEditBool.setVisibility(type == CustomPropertyType.BOOLEAN ? View.VISIBLE : View.GONE);
        mDateTime.setVisibility(type == CustomPropertyType.DATETIME ? View.VISIBLE : View.GONE);
        mValueLabel.setVisibility(type != CustomPropertyType.CLEAR ? View.VISIBLE : View.GONE);
    }

    private CustomPropertyType getType() {
        return CustomPropertyType.values()[mEditType.getSelectedItemPosition()];
    }

    public void set(CustomProperties customProperties) {
        CustomPropertyType type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case CLEAR:
                customProperties.clear(key);
                break;
            case BOOLEAN:
                customProperties.set(key, mEditBool.isChecked());
                break;
            case NUMBER:
                String stringValue = mEditNumber.getText().toString();
                Number value;
                if (!stringValue.isEmpty()) {
                    try {
                        value = Integer.parseInt(stringValue);
                    } catch (NumberFormatException ignored) {
                        value = Double.parseDouble(stringValue);
                    }
                } else {
                    value = Double.NaN;
                }
                customProperties.set(key, value);
                break;
            case DATETIME:
                customProperties.set(key, mDate);
                break;
            case STRING:
                customProperties.set(key, mEditString.getText().toString());
                break;
        }
    }

    public enum CustomPropertyType {
        STRING,
        BOOLEAN,
        NUMBER,
        DATETIME,
        CLEAR
    }
}