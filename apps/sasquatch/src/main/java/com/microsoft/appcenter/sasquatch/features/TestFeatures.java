package com.microsoft.appcenter.sasquatch.features;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.CrashActivity;
import com.microsoft.appcenter.sasquatch.activities.CustomPropertiesActivity;
import com.microsoft.appcenter.sasquatch.activities.DeviceInfoActivity;
import com.microsoft.appcenter.sasquatch.activities.DummyActivity;
import com.microsoft.appcenter.sasquatch.activities.EventActivity;
import com.microsoft.appcenter.sasquatch.activities.ManagedErrorActivity;
import com.microsoft.appcenter.sasquatch.activities.PageActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

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
        sTestFeatureModels.add(new TestFeatureTitle(R.string.crashes_title));
        sTestFeatureModels.add(new TestFeature(R.string.title_crashes, R.string.description_crashes, CrashActivity.class));
        sTestFeatureModels.add(new TestFeature(R.string.title_error, R.string.description_error, ManagedErrorActivity.class));
        sTestFeatureModels.add(new TestFeatureTitle(R.string.miscellaneous_title));
        try {
            Class classCustomProperties = Class.forName("com.microsoft.appcenter.CustomProperties");
            if (classCustomProperties != null) {
                sTestFeatureModels.add(new TestFeature(R.string.title_custom_properties, R.string.description_custom_properties, CustomPropertiesActivity.class));
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "CustomProperties not yet available in this flavor.");
        }
        sTestFeatureModels.add(new TestFeature(R.string.title_device_info, R.string.description_device_info, DeviceInfoActivity.class));
    }

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
                    model.mOnClickListener.onClick(view);
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

    abstract static class TestFeatureModel {
        private final String mTitle;

        TestFeatureModel(int title) {
            this.mTitle = title > 0 ? sParentActivity.get().getResources().getString(title) : "";
        }

        String getTitle() {
            return mTitle;
        }
    }

    static class TestFeatureTitle extends TestFeatureModel {
        TestFeatureTitle(int title) {
            super(title);
        }
    }

    static class TestFeature extends TestFeatureModel {
        private final String mDescription;
        private final View.OnClickListener mOnClickListener;

        TestFeature(int title, int description, Class<? extends Activity> clazz) {
            this(title, description, getDefaultOnClickListener(clazz));
        }

        TestFeature(int title, int description, View.OnClickListener listener) {
            super(title);
            this.mDescription = description > 0 ? sParentActivity.get().getResources().getString(description) : "";
            this.mOnClickListener = listener;
        }

        String getDescription() {
            return mDescription;
        }
    }
}
