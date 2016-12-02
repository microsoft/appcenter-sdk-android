/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.sasquatch.features;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TestFeaturesListAdapter extends BaseAdapter {
    private List<TestFeatures.TestFeatureModel> mList = new ArrayList<>();

    public TestFeaturesListAdapter(List<TestFeatures.TestFeatureModel> list) {
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
    public View getView(int position, View convertView, ViewGroup parent) {
        /* Not use view holder pattern since this is just a test app. */
        @SuppressLint("ViewHolder")
        View rowView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        TextView titleView = (TextView) rowView.findViewById(android.R.id.text1);
        TextView descriptionView = (TextView) rowView.findViewById(android.R.id.text2);

        /* Set title and description to the view. */
        TestFeatures.TestFeatureModel model = (TestFeatures.TestFeatureModel) getItem(position);
        titleView.setText(model.getTitle());
        descriptionView.setText(model.getDescription());
        return rowView;
    }
}