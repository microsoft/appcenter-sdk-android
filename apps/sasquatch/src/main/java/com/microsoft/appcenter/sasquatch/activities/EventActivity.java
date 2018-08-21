package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventActivity extends LogActivity {

    private Spinner mTransmissionTargetSpinner;

    private CheckBox mTransmissionEnabledCheckBox;

    private Button mConfigureTargetPropertiesButton;

    private Button mOverrideCommonSchemaButton;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Transmission target views init. */
        mTransmissionTargetSpinner = findViewById(R.id.transmission_target);
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.target_id_names));
        mTransmissionTargetSpinner.setAdapter(targetAdapter);
        mTransmissionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateButtonStates(getSelectedTarget());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* Init Configure target properties button. */
        mConfigureTargetPropertiesButton = findViewById(R.id.configure_button);
        mConfigureTargetPropertiesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventActivity.this, EventPropertiesActivity.class);
                intent.putExtra(ActivityConstants.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition() - 1);
                startActivity(intent);
            }
        });

        /* Init enabled check box. */
        mTransmissionEnabledCheckBox = findViewById(R.id.transmission_enabled);

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);

        /* Init common schema properties button. */
        mOverrideCommonSchemaButton = findViewById(R.id.override_cs_button);

        /* Init override common schema properties button. */
        mOverrideCommonSchemaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(EventActivity.this, CommonSchemaPropertiesActivity.class);
                intent.putExtra(ActivityConstants.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition());
                startActivity(intent);
            }
        });

        /* Test start from library. */
        AppCenter.startFromLibrary(this, Analytics.class);
    }

    @Override
    int getLayoutId() {
        return R.layout.activity_event;
    }

    @Override
    void trackLog(String name, Map<String, String> properties) {

        /* First item is always empty as it's default value which means either appcenter, one collector or both. */
        AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target == null) {
            Analytics.trackEvent(name, properties);
        } else {
            target.trackEvent(name, properties);
        }
    }

    private AnalyticsTransmissionTarget getSelectedTarget() {
        return mTransmissionTargets.get(mTransmissionTargetSpinner.getSelectedItemPosition());
    }

    public void toggleTransmissionEnabled(View view) {
        boolean checked = mTransmissionEnabledCheckBox.isChecked();
        final AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target != null) {
            target.setEnabledAsync(checked);
            updateButtonStates(target);
        }
    }

    private void updateButtonStates(AnalyticsTransmissionTarget target) {
        if (target == null) {
            mTransmissionEnabledCheckBox.setVisibility(View.GONE);
            mConfigureTargetPropertiesButton.setVisibility(View.GONE);
            mOverrideCommonSchemaButton.setVisibility(View.GONE);
        } else {
            mTransmissionEnabledCheckBox.setVisibility(View.VISIBLE);
            mConfigureTargetPropertiesButton.setVisibility(View.VISIBLE);
            mOverrideCommonSchemaButton.setVisibility(View.VISIBLE);
            boolean enabled = target.isEnabledAsync().get();
            mTransmissionEnabledCheckBox.setChecked(enabled);
            if (enabled) {
                mTransmissionEnabledCheckBox.setText(R.string.transmission_enabled);
            } else {
                mTransmissionEnabledCheckBox.setText(R.string.transmission_disabled);
            }
        }
    }
}
