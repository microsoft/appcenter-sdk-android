/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.EventProperties;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_EXPIRES_IN_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.MSA_USER_ID_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.sSharedPreferences;

public class EventActivity extends PropertyActivity {

    /**
     * Remember for what targets the device id was enabled.
     * It shouldn't be lost on recreate activity.
     */
    private static final Set<AnalyticsTransmissionTarget> DEVICE_ID_ENABLED = new HashSet<>();

    private TextView mName;

    private Spinner mTransmissionTargetSpinner;

    private CheckBox mTransmissionEnabledCheckBox;

    private CheckBox mDeviceIdEnabledCheckBox;

    private Button mConfigureTargetPropertiesButton;

    private Button mOverrideCommonSchemaButton;

    private Button mPauseTransmissionButton;

    private Button mResumeTransmissionButton;

    private Spinner mPersistenceFlagSpinner;

    private TextView mNumberOfLogsLabel;

    private SeekBar mNumberOfLogs;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_log_top, ((LinearLayout) findViewById(R.id.top_layout)));
        getLayoutInflater().inflate(R.layout.layout_event, ((LinearLayout) findViewById(R.id.middle_layout)));

        /* Test start from library. */
        AppCenter.startFromLibrary(this, Analytics.class);

        /* Init name field. */
        mName = findViewById(R.id.name);

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

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);

        String msaUserId = sSharedPreferences.getString(MSA_USER_ID_KEY, null);
        if (msaUserId != null) {
            long expirationDate = sSharedPreferences.getLong(MSA_EXPIRES_IN_KEY, 0);
            Calendar calendar = Calendar.getInstance();
            if (calendar.getTimeInMillis() > expirationDate) {
                sSharedPreferences.edit().remove(MSA_EXPIRES_IN_KEY).apply();
                sSharedPreferences.edit().remove(MSA_USER_ID_KEY).apply();
            } else {
                for(AnalyticsTransmissionTarget target : mTransmissionTargets) {
                    target.getPropertyConfigurator().setUserId(msaUserId);
                }
            }
        }

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

        /* Init pause/resume buttons. */
        mPauseTransmissionButton = findViewById(R.id.pause_transmission_button);
        mPauseTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getSelectedTarget().pause();
            }
        });
        mResumeTransmissionButton = findViewById(R.id.resume_transmission_button);
        mResumeTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getSelectedTarget().resume();
            }
        });

        /* Persistence flag. */
        mPersistenceFlagSpinner = findViewById(R.id.event_priority_spinner);
        ArrayAdapter<String> persistenceFlagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.event_priority_values));
        mPersistenceFlagSpinner.setAdapter(persistenceFlagAdapter);

        /* Number of logs. */
        mNumberOfLogsLabel = findViewById(R.id.number_of_logs_label);
        mNumberOfLogs = findViewById(R.id.number_of_logs);
        mNumberOfLogsLabel.setText(String.format(getString(R.string.number_of_logs), getNumberOfLogs()));
        mNumberOfLogs.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mNumberOfLogsLabel.setText(String.format(getString(R.string.number_of_logs), getNumberOfLogs()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    private int getNumberOfLogs() {
        return Math.max(mNumberOfLogs.getProgress(), 1);
    }

    private boolean onlyStringProperties() {
        for (TypedPropertyFragment fragment : mProperties) {
            if (fragment.getType() != TypedPropertyFragment.EventPropertyType.STRING) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void send(View view) {
        AnalyticsTransmissionTarget target = getSelectedTarget();
        PersistenceFlag persistenceFlag = PersistenceFlag.values()[mPersistenceFlagSpinner.getSelectedItemPosition()];
        int flags = getFlags(persistenceFlag);
        String name = mName.getText().toString();
        Map<String, String> properties = null;
        EventProperties typedProperties = null;
        if (mProperties.size() > 0) {
            if (onlyStringProperties()) {
                properties = readStringProperties();
            } else {
                typedProperties = new EventProperties();
                for (TypedPropertyFragment fragment : mProperties) {
                    fragment.set(typedProperties);
                }
            }
        }

        /* First item is always empty as it's default value which means either AppCenter, one collector or both. */
        for (int i = 0; i < getNumberOfLogs(); i++) {
            boolean useExplicitFlags = persistenceFlag != PersistenceFlag.DEFAULT;
            if (target == null) {
                if (properties != null) {
                    if (useExplicitFlags) {
                        Analytics.trackEvent(name, properties, flags);
                    } else {
                        Analytics.trackEvent(name, properties);
                    }
                } else if (typedProperties != null || useExplicitFlags) {
                    if (useExplicitFlags) {
                        Analytics.trackEvent(name, typedProperties, flags);
                    } else {
                        Analytics.trackEvent(name, typedProperties);
                    }
                } else {
                    Analytics.trackEvent(name);
                }
            } else {
                if (properties != null) {
                    if (useExplicitFlags) {
                        target.trackEvent(name, properties, flags);
                    } else {
                        target.trackEvent(name, properties);
                    }
                } else if (typedProperties != null || useExplicitFlags) {
                    if (useExplicitFlags) {
                        target.trackEvent(name, typedProperties, flags);
                    } else {
                        target.trackEvent(name, typedProperties);
                    }
                } else {
                    target.trackEvent(name);
                }
            }
        }
    }

    private int getFlags(PersistenceFlag persistenceFlag) {
        switch (persistenceFlag) {
            case DEFAULT:
                return Flags.getPersistenceFlag(Flags.DEFAULTS, true);

            case NORMAL:
                return Flags.PERSISTENCE_NORMAL;

            case CRITICAL:
                return Flags.PERSISTENCE_CRITICAL;

            case INVALID:
                return 42;
        }
        throw new IllegalArgumentException();
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
            target.getPropertyConfigurator().collectDeviceId();
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
            mPauseTransmissionButton.setVisibility(View.VISIBLE);
            mResumeTransmissionButton.setVisibility(View.VISIBLE);
            boolean enabled = target.isEnabledAsync().get();
            mTransmissionEnabledCheckBox.setChecked(enabled);
            mTransmissionEnabledCheckBox.setText(enabled ? R.string.transmission_enabled : R.string.transmission_disabled);
            boolean deviceIdEnabled = DEVICE_ID_ENABLED.contains(target);
            mDeviceIdEnabledCheckBox.setVisibility(View.VISIBLE);
            mDeviceIdEnabledCheckBox.setChecked(deviceIdEnabled);
            mDeviceIdEnabledCheckBox.setText(deviceIdEnabled ? R.string.device_id_enabled : R.string.device_id_disabled);
            mDeviceIdEnabledCheckBox.setEnabled(!deviceIdEnabled);
        }
    }

    private enum PersistenceFlag {
        DEFAULT,
        NORMAL,
        CRITICAL,
        INVALID
    }
}
