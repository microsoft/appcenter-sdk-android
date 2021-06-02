/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.features;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.AuthenticationProviderActivity;
import com.microsoft.appcenter.sasquatch.activities.CrashActivity;
import com.microsoft.appcenter.sasquatch.activities.CustomPropertiesActivity;
import com.microsoft.appcenter.sasquatch.activities.DeviceInfoActivity;
import com.microsoft.appcenter.sasquatch.activities.DummyActivity;
import com.microsoft.appcenter.sasquatch.activities.EventActivity;
import com.microsoft.appcenter.sasquatch.activities.MainActivity;
import com.microsoft.appcenter.sasquatch.activities.ManagedErrorActivity;
import com.microsoft.appcenter.sasquatch.activities.PageActivity;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class TestFeatures {

    private static List<TestFeatureModel> sTestFeatureModels;

    private static WeakReference<Activity> sParentActivity;

    public static void initialize(Activity parentActivity) {
        sTestFeatureModels = new ArrayList<>();
        sParentActivity = new WeakReference<>(parentActivity);
        sTestFeatureModels.add(new TestFeatureTitle(R.string.analytics_title));
        sTestFeatureModels.add(new TestFeature(R.string.title_event, R.string.description_event, EventActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_page, R.string.description_page, PageActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_generate_page_log, R.string.description_generate_page_log, DummyActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_auth, R.string.description_auth, AuthenticationProviderActivity.class));
        sTestFeatureModels.add(new TestFeatureTitle(R.string.crashes_title));
        sTestFeatureModels.add(new TestFeature(R.string.title_crashes, R.string.description_crashes, CrashActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_error, R.string.description_error, ManagedErrorActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_had_memory_warning, hadMemoryWarning() ? R.string.description_had_memory_warning : R.string.description_did_not_have_memory_warning));
        sTestFeatureModels.add(new TestFeatureTitle(R.string.miscellaneous_title));
        sTestFeatureModels.add(new TestFeature(R.string.title_custom_properties, R.string.description_custom_properties, CustomPropertiesActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_device_info, R.string.description_device_info, DeviceInfoActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_check_for_update, R.string.description_check_for_update, checkForUpdateClickListener));
    }

    @SuppressWarnings("unchecked")
    private static boolean hadMemoryWarning() {
        return Crashes.hasReceivedMemoryWarningInLastSession().get();
    }

    private static View.OnClickListener checkForUpdateClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Distribute.checkForUpdate();
        }
    };

    public static List<TestFeatureModel> getAvailableControls() {
        return sTestFeatureModels;
    }

    public static AdapterView.OnItemClickListener getOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof TestFeature) {
                    TestFeature model = (TestFeature) item;
                    if (model.mOnClickListener != null) {
                        model.mOnClickListener.onClick(view);
                    }
                }
            }
        };
    }

    private static View.OnClickListener getDefaultOnClickListener(final Class<? extends Activity> clazz) {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sParentActivity.get().startActivity(new Intent(sParentActivity.get(), clazz));
            }
        };
    }

    public abstract static class TestFeatureModel {

        private final String mTitle;

        TestFeatureModel(int title) {
            this.mTitle = title > 0 ? sParentActivity.get().getResources().getString(title) : "";
        }

        String getTitle() {
            return mTitle;
        }
    }

    public static class TestFeatureTitle extends TestFeatureModel {

        public TestFeatureTitle(int title) {
            super(title);
        }
    }

    public static class TestFeature extends TestFeatureModel {

        private final String mDescription;

        private final View.OnClickListener mOnClickListener;

        TestFeature(int title, int description) {
            this(title, description, (View.OnClickListener)null);
        }

        TestFeature(int title, int description, Class<? extends Activity> clazz) {
            this(title, description, getDefaultOnClickListener(clazz));
        }

        public TestFeature(int title, int description, @Nullable View.OnClickListener listener) {
            super(title);
            this.mDescription = description > 0 ? sParentActivity.get().getResources().getString(description) : "";
            this.mOnClickListener = listener;
        }

        String getDescription() {
            return mDescription;
        }
    }
}
