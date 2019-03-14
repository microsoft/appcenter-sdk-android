/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.PropertyConfigurator;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.lang.reflect.Method;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.activities.ActivityConstants.EXTRA_TARGET_SELECTED;

public class CommonSchemaPropertiesActivity extends AppCompatActivity {

    private Spinner mCommonSchemaPropertiesSpinner;

    private EditText mCommonSchemaPropertyValue;

    private PropertyConfigurator mPropertyConfigurator;

    private enum PropertyName {
        APP_NAME,
        APP_VERSION,
        APP_LOCALE,
        USER_ID
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_schema_properties);

        /* Common schema properties init. */
        mCommonSchemaPropertiesSpinner = findViewById(R.id.common_schema_property);
        ArrayAdapter<String> csAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.common_schema_properties));
        mCommonSchemaPropertiesSpinner.setAdapter(csAdapter);
        mCommonSchemaPropertiesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                PropertyName propertyName = PropertyName.values()[position];
                String methodName;
                switch (propertyName) {

                    case APP_NAME:
                        methodName = "getAppName";
                        break;

                    case APP_VERSION:
                        methodName = "getAppVersion";
                        break;

                    case APP_LOCALE:
                        methodName = "getAppLocale";
                        break;

                    case USER_ID:
                        methodName = "getUserId";
                        break;

                    default:
                        throw new IllegalStateException();
                }
                try {
                    Method getValue = mPropertyConfigurator.getClass().getDeclaredMethod(methodName);
                    getValue.setAccessible(true);
                    String value = (String) getValue.invoke(mPropertyConfigurator);
                    mCommonSchemaPropertyValue.setText(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /* Common schema properties value listener. */
        mCommonSchemaPropertyValue = findViewById(R.id.common_schema_property_entry);

        /* Get a reference to the transmission target that was selected in the previous screen. */
        List<AnalyticsTransmissionTarget> transmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);
        int selectedTargetIndex = getIntent().getIntExtra(EXTRA_TARGET_SELECTED, 0);
        mPropertyConfigurator = transmissionTargets.get(selectedTargetIndex).getPropertyConfigurator();
    }

    @SuppressWarnings("unused")
    public void saveProperty(View view) {
        PropertyName propertyName = PropertyName.values()[mCommonSchemaPropertiesSpinner.getSelectedItemPosition()];
        String value = mCommonSchemaPropertyValue.getText().toString();
        if (TextUtils.isEmpty(value)) {
            value = null;
        }
        switch (propertyName) {

            case APP_NAME:
                mPropertyConfigurator.setAppName(value);
                break;

            case APP_VERSION:
                mPropertyConfigurator.setAppVersion(value);
                break;

            case APP_LOCALE:
                mPropertyConfigurator.setAppLocale(value);
                break;

            case USER_ID:
                mPropertyConfigurator.setUserId(value);
                break;
        }
        Toast.makeText(this, R.string.property_saved, Toast.LENGTH_SHORT).show();
    }
}
