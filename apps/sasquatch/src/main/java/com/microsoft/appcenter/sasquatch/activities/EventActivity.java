package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

/**
 * TODO remove reflection once new APIs available in jCenter.
 */
public class EventActivity extends LogActivity {

    private Spinner mTransmissionTargetSpinner;

    private CheckBox mTransmissionEnabledCheckBox;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    private Method mIsEnabledMethod;

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Transmission target views init. */
        mTransmissionTargetSpinner = findViewById(R.id.transmission_target);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.target_id_names));
        mTransmissionTargetSpinner.setAdapter(adapter);
        mTransmissionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTransmissionEnabledCheckBox(getSelectedTarget());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* Init Configure target properties button. */
        Method method;
        try {
            method = AnalyticsTransmissionTarget.class.getMethod("setEventProperty", String.class, String.class);
        } catch (Exception e) {
            method = null;
        }
        if (method == null) {
            findViewById(R.id.configure_button).setVisibility(GONE);
        } else {
            findViewById(R.id.configure_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(EventActivity.this, EventPropertiesActivity.class);
                    intent.putExtra(EventPropertiesActivity.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition());
                    startActivity(intent);
                }
            });
        }

        /* Init enabled check box. */
        mTransmissionEnabledCheckBox = findViewById(R.id.transmission_enabled);
        try {
            mIsEnabledMethod = AnalyticsTransmissionTarget.class.getMethod("isEnabledAsync");
        } catch (NoSuchMethodException e) {
            mIsEnabledMethod = null;
        }

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);

        /* Test start from library. */
        try {
            Method startFromLibrary = AppCenter.class.getMethod("startFromLibrary", Context.class, Class[].class);
            startFromLibrary.invoke(null, this, new Class[]{Analytics.class});
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public void toggleTransmissionEnabled(View view) throws Exception {
        boolean checked = mTransmissionEnabledCheckBox.isChecked();
        final AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target != null) {

            @SuppressWarnings("JavaReflectionMemberAccess")
            Method method = AnalyticsTransmissionTarget.class.getMethod("setEnabledAsync", boolean.class);
            method.invoke(target, checked);
            updateTransmissionEnabledCheckBox(target);
        }
    }

    private void updateTransmissionEnabledCheckBox(AnalyticsTransmissionTarget target) {
        if (target == null || mIsEnabledMethod == null) {
            mTransmissionEnabledCheckBox.setVisibility(GONE);
        } else {
            mTransmissionEnabledCheckBox.setVisibility(View.VISIBLE);
            boolean enabled = isTransmissionEnabled(target);
            mTransmissionEnabledCheckBox.setChecked(enabled);
            if (enabled) {
                mTransmissionEnabledCheckBox.setText(R.string.transmission_enabled);
            } else {
                mTransmissionEnabledCheckBox.setText(R.string.transmission_disabled);
            }
        }
    }

    private boolean isTransmissionEnabled(AnalyticsTransmissionTarget target) {
        try {
            return (boolean) ((AppCenterFuture) mIsEnabledMethod.invoke(target)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
