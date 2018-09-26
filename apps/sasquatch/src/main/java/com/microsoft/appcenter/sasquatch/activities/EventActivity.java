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
import com.microsoft.appcenter.analytics.PropertyConfigurator;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventActivity extends LogActivity {

    /**
     * Remember for what targets the device id was enabled.
     * It shouldn't be lost on recreate activity.
     */
    private static Set<AnalyticsTransmissionTarget> DEVICE_ID_ENABLED = new HashSet<>();

    private Spinner mTransmissionTargetSpinner;

    private CheckBox mTransmissionEnabledCheckBox;

    private CheckBox mDeviceIdEnabledCheckBox;

    private Button mConfigureTargetPropertiesButton;

    private Button mOverrideCommonSchemaButton;

    private Button mPauseTransmissionButton;

    private Button mResumeTransmissionButton;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    // TODO remove reflection once new APIs available in jCenter.
    private Method mCollectDeviceIdMethod;

    // TODO remove reflection once new APIs available in jCenter.
    private Method mPauseMethod;

    // TODO remove reflection once new APIs available in jCenter.
    private Method mResumeMethod;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Test start from library. */
        AppCenter.startFromLibrary(this, Analytics.class);

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

        /* Init enabled check boxes. */
        mTransmissionEnabledCheckBox = findViewById(R.id.transmission_enabled);
        mDeviceIdEnabledCheckBox = findViewById(R.id.device_id_enabled);
        try {
            //noinspection JavaReflectionMemberAccess
            mCollectDeviceIdMethod = PropertyConfigurator.class.getMethod("collectDeviceId");
        } catch (NoSuchMethodException e) {
            mDeviceIdEnabledCheckBox.setVisibility(View.GONE);
        }

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
                Intent intent = new Intent(EventActivity.this, CommonSchemaPropertiesActivity.class);
                intent.putExtra(ActivityConstants.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition());
                startActivity(intent);
            }
        });

        /* Init pause/resume buttons. TODO remove reflection once SDKs are released. */
        mPauseTransmissionButton = findViewById(R.id.pause_transmission_button);
        try {
            //noinspection JavaReflectionMemberAccess
            mPauseMethod = AnalyticsTransmissionTarget.class.getMethod("pause");
        } catch (NoSuchMethodException e) {
            mPauseTransmissionButton.setVisibility(View.GONE);
        }
        mPauseTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            @SuppressWarnings("ConstantConditions")
            public void onClick(View v) {
                try {
                    mPauseMethod.invoke(getSelectedTarget());
                } catch (IllegalAccessException ignored) {
                } catch (InvocationTargetException ignored) {
                }
            }
        });
        mResumeTransmissionButton = findViewById(R.id.resume_transmission_button);
        try {
            //noinspection JavaReflectionMemberAccess
            mResumeMethod = AnalyticsTransmissionTarget.class.getMethod("resume");
        } catch (NoSuchMethodException e) {
            mResumeTransmissionButton.setVisibility(View.GONE);
        }
        mResumeTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            @SuppressWarnings("ConstantConditions")
            public void onClick(View v) {
                try {
                    mResumeMethod.invoke(getSelectedTarget());
                } catch (IllegalAccessException ignored) {
                } catch (InvocationTargetException ignored) {
                }
            }
        });
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

    public void toggleDeviceIdEnabled(View view) {
        final AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target != null) {
            updateButtonStates(target);

            // TODO remove reflection once new APIs available in jCenter.
            // target.getPropertyConfigurator().collectDeviceId();
            try {
                mCollectDeviceIdMethod.invoke(target.getPropertyConfigurator());
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }
            mDeviceIdEnabledCheckBox.setChecked(true);
            mDeviceIdEnabledCheckBox.setText(R.string.device_id_enabled);
            mDeviceIdEnabledCheckBox.setEnabled(false);
            DEVICE_ID_ENABLED.add(target);
        }
    }

    private void updateButtonStates(AnalyticsTransmissionTarget target) {
        if (target == null) {
            mTransmissionEnabledCheckBox.setVisibility(View.GONE);
            mConfigureTargetPropertiesButton.setVisibility(View.GONE);
            mOverrideCommonSchemaButton.setVisibility(View.GONE);
            mDeviceIdEnabledCheckBox.setVisibility(View.GONE);
            mPauseTransmissionButton.setVisibility(View.GONE);
            mResumeTransmissionButton.setVisibility(View.GONE);
        } else {
            mTransmissionEnabledCheckBox.setVisibility(View.VISIBLE);
            mConfigureTargetPropertiesButton.setVisibility(View.VISIBLE);
            mOverrideCommonSchemaButton.setVisibility(View.VISIBLE);
            if (mPauseMethod != null) {
                mPauseTransmissionButton.setVisibility(View.VISIBLE);
            }
            if (mResumeMethod != null) {
                mResumeTransmissionButton.setVisibility(View.VISIBLE);
            }
            boolean enabled = target.isEnabledAsync().get();
            mTransmissionEnabledCheckBox.setChecked(enabled);
            mTransmissionEnabledCheckBox.setText(enabled ? R.string.transmission_enabled : R.string.transmission_disabled);
            boolean deviceIdEnabled = DEVICE_ID_ENABLED.contains(target);
            if (mCollectDeviceIdMethod != null) {
                mDeviceIdEnabledCheckBox.setVisibility(View.VISIBLE);
            }
            mDeviceIdEnabledCheckBox.setChecked(deviceIdEnabled);
            mDeviceIdEnabledCheckBox.setText(deviceIdEnabled ? R.string.device_id_enabled : R.string.device_id_disabled);
            mDeviceIdEnabledCheckBox.setEnabled(!deviceIdEnabled);
        }
    }
}
