/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;

import java.util.ArrayList;

public class CustomItemAdapter extends BaseAdapter implements ListAdapter {

    private ArrayList<String> mList;

    private Context mContext;

    CustomItemAdapter(ArrayList<String> list, Context context) {
        mList = list;
        mContext = context;
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
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater != null) {
                view = inflater.inflate(R.layout.item_view_property, parent, false);
            }
        }
        if (view != null) {
            TextView listItemText = view.findViewById(R.id.property);
            listItemText.setText(mList.get(position));
            ImageButton deleteBtn = view.findViewById(R.id.delete_button);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mList.remove(position);
                    notifyDataSetChanged();
                }
            });
        }
        return view;
    }
}
