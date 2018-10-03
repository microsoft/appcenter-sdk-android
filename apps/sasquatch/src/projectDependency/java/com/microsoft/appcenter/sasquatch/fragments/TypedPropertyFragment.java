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
import com.microsoft.appcenter.sasquatch.R;

import java.util.Map;

// TODO move to main folder once new APIs available in jCenter
public class TypedPropertyFragment extends EditDateTimeFragment {

    public static final int TYPE_BOOLEAN = 0;
    public static final int TYPE_NUMBER_DOUBLE = 1;
    public static final int TYPE_NUMBER_LONG = 2;
    public static final int TYPE_DATETIME = 3;
    public static final int TYPE_STRING = 4;

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

        mEditKey = view.findViewById(R.id.key);
        mEditType = view.findViewById(R.id.type);
        mEditString = view.findViewById(R.id.string);
        mEditNumberDouble = view.findViewById(R.id.number_double);
        mEditNumberLong = view.findViewById(R.id.number_long);
        mEditBool = view.findViewById(R.id.bool);

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
        int type = getType();
        mEditString.setVisibility(type == TYPE_STRING ? View.VISIBLE : View.GONE);
        mEditBool.setVisibility(type == TYPE_BOOLEAN ? View.VISIBLE : View.GONE);
        mEditNumberDouble.setVisibility(type == TYPE_NUMBER_DOUBLE ? View.VISIBLE : View.GONE);
        mEditNumberLong.setVisibility(type == TYPE_NUMBER_LONG ? View.VISIBLE : View.GONE);
        mDateTime.setVisibility(type == TYPE_DATETIME ? View.VISIBLE : View.GONE);
    }

    public int getType() {
        return mEditType.getSelectedItemPosition();
    }

    public void set(Map<String, String> eventProperties) {
        int type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case TYPE_STRING:
                eventProperties.put(key, mEditString.getText().toString());
                break;
        }
    }

    public void set(EventProperties eventProperties) {
        int type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case TYPE_BOOLEAN:
                eventProperties.set(key, mEditBool.isChecked());
                break;
            case TYPE_NUMBER_DOUBLE: {
                String stringValue = mEditNumberDouble.getText().toString();
                Double value = Double.parseDouble(stringValue);
                eventProperties.set(key, value);
                break;
            }
            case TYPE_NUMBER_LONG: {
                String stringValue = mEditNumberLong.getText().toString();
                Long value = Long.parseLong(stringValue);
                eventProperties.set(key, value);
                break;
            }
            case TYPE_DATETIME:
                eventProperties.set(key, mDate);
                break;
            case TYPE_STRING:
                eventProperties.set(key, mEditString.getText().toString());
                break;
        }
    }
}
