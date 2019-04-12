/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;

import java.util.ArrayList;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_CONTENTS;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_DOCUMENT_LIST;

public class CustomItemAdapter extends RecyclerView.Adapter<CustomItemAdapter.CustomItemAdapterHolder> {

    private ArrayList<String> mList;

    private Context mContext;

    CustomItemAdapter(ArrayList<String> list, Context context) {
        mList = list;
        mContext = context;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public CustomItemAdapterHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CustomItemAdapterHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_property, null, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomItemAdapterHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, UserDocumentDetailActivity.class);
                intent.putExtra(USER_DOCUMENT_LIST, loadArrayFromPreferences(mContext, USER_DOCUMENT_LIST).get(position));
                intent.putExtra(USER_DOCUMENT_CONTENTS, loadArrayFromPreferences(mContext, USER_DOCUMENT_CONTENTS).get(position));
                mContext.startActivity(intent);
            }
        });
        holder.listItemText.setText(mList.get(position));
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Storage.delete(Constants.USER, StorageActivity.sUserDocumentList.get(position));
                mList.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mList.size();
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

    public class CustomItemAdapterHolder extends RecyclerView.ViewHolder {

        public TextView listItemText;
        public ImageButton deleteBtn;

        public CustomItemAdapterHolder(@NonNull View itemView) {
            super(itemView);
            listItemText = itemView.findViewById(R.id.property);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }

}
