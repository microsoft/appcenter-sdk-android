package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
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
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        /* Init enabled check box. */
        mTransmissionEnabledCheckBox = findViewById(R.id.transmission_enabled);
        Method getTransmissionTargetMethod;
        try {
            mIsEnabledMethod = AnalyticsTransmissionTarget.class.getMethod("isEnabledAsync");
            getTransmissionTargetMethod = AnalyticsTransmissionTarget.class.getMethod("getTransmissionTarget", String.class);
        } catch (NoSuchMethodException e) {
            mIsEnabledMethod = null;
            getTransmissionTargetMethod = null;
        }

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        String[] targetTokens = getResources().getStringArray(R.array.target_id_values);
        mTransmissionTargets.add(null);
        mTransmissionTargets.add(Analytics.getTransmissionTarget(targetTokens[1]));
        for (int i = 2; i < targetTokens.length; i++) {
            String targetToken = targetTokens[i];
            AnalyticsTransmissionTarget target;
            if (getTransmissionTargetMethod == null) {
                target = Analytics.getTransmissionTarget(targetToken);
            } else {
                try {
                    target = (AnalyticsTransmissionTarget) getTransmissionTargetMethod.invoke(mTransmissionTargets.get(i - 1), targetToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            mTransmissionTargets.add(target);
        }

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
            mTransmissionEnabledCheckBox.setVisibility(View.GONE);
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
