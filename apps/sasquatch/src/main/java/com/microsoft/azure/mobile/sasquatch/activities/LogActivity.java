package com.microsoft.azure.mobile.sasquatch.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.azure.mobile.sasquatch.R;

import java.util.HashMap;
import java.util.Map;

public abstract class LogActivity extends AppCompatActivity implements TextWatcher {

    private TextView mLastInput;

    private ViewGroup mList;

    private LayoutInflater mLayoutInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mList = (ViewGroup) findViewById(R.id.list);
        addProperty();
    }

    private void addProperty() {
        if (mLastInput != null)
            mLastInput.removeTextChangedListener(this);
        View view = mLayoutInflater.inflate(R.layout.property, mList, false);
        mList.addView(view);
        mLastInput = (TextView) view.findViewById(R.id.value);
        mLastInput.addTextChangedListener(this);
    }

    @SuppressWarnings("unused")
    public void send(@SuppressWarnings("UnusedParameters") View view) {
        String name = ((TextView) findViewById(R.id.name)).getText().toString();
        Map<String, String> properties = null;
        for (int i = 0; i < mList.getChildCount(); i++) {
            View childAt = mList.getChildAt(i);
            CharSequence key = ((TextView) childAt.findViewById(R.id.key)).getText();
            CharSequence value = ((TextView) childAt.findViewById(R.id.value)).getText();
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                if (properties == null)
                    properties = new HashMap<>();
                properties.put(key.toString(), value.toString());
            }
        }
        trackLog(name, properties);
    }

    protected abstract void trackLog(String name, Map<String, String> properties);

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0)
            addProperty();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
