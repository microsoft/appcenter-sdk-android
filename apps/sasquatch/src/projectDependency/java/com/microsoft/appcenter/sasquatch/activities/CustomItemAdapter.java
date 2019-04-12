/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;

import java.util.ArrayList;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_CONTENTS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_LIST;

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
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, UserDocumentDetailActivity.class);
                    intent.putExtra(USER_DOCUMENT_LIST, loadArrayFromPreferences(mContext, USER_DOCUMENT_LIST).get(position));
                    intent.putExtra(USER_DOCUMENT_CONTENTS, loadArrayFromPreferences(mContext, USER_DOCUMENT_CONTENTS).get(position));
                    mContext.startActivity(intent);
                }
            });
            TextView listItemText = view.findViewById(R.id.property);
            listItemText.setText(mList.get(position));
            ImageButton deleteBtn = view.findViewById(R.id.delete_button);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Storage.delete(Constants.USER, StorageActivity.sUserDocumentList.get(position));
                    mList.remove(position);
                    notifyDataSetChanged();
                }
            });
        }
        return view;
    }

    public ArrayList<String> loadArrayFromPreferences(Context context, String name) {
        MainActivity.sSharedPreferences = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
        ArrayList<String> list = new ArrayList<>();
        int size = MainActivity.sSharedPreferences.getInt("Status_size", 0);
        for (int i = 0; i < size; i++) {
            list.add(MainActivity.sSharedPreferences.getString("Status_" + i, null));
        }
        return list;
    }

}
