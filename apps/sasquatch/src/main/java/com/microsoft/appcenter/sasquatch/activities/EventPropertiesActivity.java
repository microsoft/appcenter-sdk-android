/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.EventProperties;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.ActivityConstants.EXTRA_TARGET_SELECTED;

public class EventPropertiesActivity extends AppCompatActivity {

    private Spinner mTransmissionTargetSpinner;

    private ListView mListView;

    private LinearLayout mAddPropertyLayout;

    private TypedPropertyFragment mTypedPropertyFragment;

    private PropertyListAdapter mPropertyListAdapter;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_properties);

        /* Initialize spinner for transmission targets. */
        mTransmissionTargetSpinner = findViewById(R.id.transmission_target);
        String[] allTargetNames = getResources().getStringArray(R.array.target_id_names);
        String[] nonDefaultTargetNames = new String[allTargetNames.length - 1];
        System.arraycopy(allTargetNames, 1, nonDefaultTargetNames, 0, nonDefaultTargetNames.length);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nonDefaultTargetNames);
        mTransmissionTargetSpinner.setAdapter(adapter);
        mTransmissionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePropertyList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mTransmissionTargetSpinner.setSelection(getIntent().getIntExtra(EXTRA_TARGET_SELECTED, 0));

        /* Initialize layout for a new property. */
        mAddPropertyLayout = findViewById(R.id.add_property);

        mTypedPropertyFragment = (TypedPropertyFragment) getSupportFragmentManager().findFragmentById(R.id.typed_property);

        /* Initialize list view. */
        mListView = findViewById(R.id.list);
        mPropertyListAdapter = new PropertyListAdapter(new ArrayList<Pair<String, Object>>());

        /*
         * Initialize analytics transmission targets.
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);
        mTransmissionTargets.remove(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            mTypedPropertyFragment.reset();
            mAddPropertyLayout.setVisibility(View.VISIBLE);
            mAddPropertyLayout.findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mTypedPropertyFragment.set(getSelectedTarget().getPropertyConfigurator());
                    updatePropertyList();
                    mAddPropertyLayout.setVisibility(View.GONE);
                }
            });
            mAddPropertyLayout.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mAddPropertyLayout.setVisibility(View.GONE);
                }
            });
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void updatePropertyList() {
        try {
            Field field = getSelectedTarget().getPropertyConfigurator().getClass().getDeclaredField("mEventProperties");
            field.setAccessible(true);
            EventProperties eventProperties;
            try {
                eventProperties = (EventProperties) field.get(getSelectedTarget().getPropertyConfigurator());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Method method = eventProperties.getClass().getDeclaredMethod("getProperties");
            method.setAccessible(true);
            Map<String, TypedProperty> properties = (Map<String, TypedProperty>) method.invoke(eventProperties);
            mPropertyListAdapter.mList.clear();
            for (Map.Entry<String, TypedProperty> entry : properties.entrySet()) {
                Object value = entry.getValue().getClass().getMethod("getValue").invoke(entry.getValue());
                mPropertyListAdapter.mList.add(new Pair<>(entry.getKey(), value));
            }
            mListView.setAdapter(mPropertyListAdapter);
            mPropertyListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AnalyticsTransmissionTarget getSelectedTarget() {
        return mTransmissionTargets.get(mTransmissionTargetSpinner.getSelectedItemPosition());
    }

    private class PropertyListAdapter extends BaseAdapter {

        private final static String KEY_VALUE_PAIR_FORMAT = "%s:%s";

        private final List<Pair<String, Object>> mList;

        private PropertyListAdapter(List<Pair<String, Object>> list) {
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @SuppressWarnings("unchecked")
        public View getView(int position, View convertView, ViewGroup parent) {

            /* Set key and value strings to the view. */
            View rowView;
            final Pair<String, Object> item = (Pair<String, Object>) getItem(position);
            ViewHolder holder;
            if (convertView != null && convertView.getTag() != null) {
                holder = (ViewHolder) convertView.getTag();
                rowView = convertView;
            } else {
                rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_property, parent, false);
                TextView textView = rowView.findViewById(R.id.property);
                ImageButton imageButton = rowView.findViewById(R.id.delete_button);
                holder = new ViewHolder(textView, imageButton);
                rowView.setTag(holder);
            }
            Object value = item.second;
            if (value instanceof String || value instanceof Date) {
                value = JSONObject.quote(value.toString());
            }
            holder.mTextView.setText(String.format(KEY_VALUE_PAIR_FORMAT, JSONObject.quote(item.first), value.toString()));
            holder.mRemoveButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    AnalyticsTransmissionTarget target = getSelectedTarget();
                    if (target != null) {
                        mList.remove(item);
                        target.getPropertyConfigurator().removeEventProperty(item.first);
                        notifyDataSetChanged();
                    }
                }
            });
            return rowView;
        }

        private class ViewHolder {

            private final TextView mTextView;

            private final ImageButton mRemoveButton;

            private ViewHolder(TextView textView, ImageButton removeButton) {
                mTextView = textView;
                mRemoveButton = removeButton;
            }
        }
    }
}
