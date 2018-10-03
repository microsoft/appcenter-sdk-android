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

    public static final int TYPE_CLEAR = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_DATETIME = 3;
    public static final int TYPE_STRING = 4;

    private EditText mEditKey;
    private Spinner mEditType;
    private EditText mEditString;
    private EditText mEditNumber;
    private CheckBox mEditBool;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.custom_property, container, false);

        mEditKey = view.findViewById(R.id.key);
        mEditType = view.findViewById(R.id.type);
        mEditString = view.findViewById(R.id.string);
        mEditNumber = view.findViewById(R.id.number);
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
        mEditNumber.setVisibility(type == TYPE_NUMBER ? View.VISIBLE : View.GONE);
        mEditBool.setVisibility(type == TYPE_BOOLEAN ? View.VISIBLE : View.GONE);
        mDateTime.setVisibility(type == TYPE_DATETIME ? View.VISIBLE : View.GONE);
    }

    public int getType() {
        return mEditType.getSelectedItemPosition();
    }

    public void set(CustomProperties customProperties) {
        int type = getType();
        String key = mEditKey.getText().toString();
        switch (type) {
            case TYPE_CLEAR:
                customProperties.clear(key);
                break;
            case TYPE_BOOLEAN:
                customProperties.set(key, mEditBool.isChecked());
                break;
            case TYPE_NUMBER:
                String stringValue = mEditNumber.getText().toString();
                Number value;
                try {
                    value = Integer.parseInt(stringValue);
                } catch (NumberFormatException ignored) {
                    value = Double.parseDouble(stringValue);
                }
                customProperties.set(key, value);
                break;
            case TYPE_DATETIME:
                customProperties.set(key, mDate);
                break;
            case TYPE_STRING:
                customProperties.set(key, mEditString.getText().toString());
                break;
        }
    }
}