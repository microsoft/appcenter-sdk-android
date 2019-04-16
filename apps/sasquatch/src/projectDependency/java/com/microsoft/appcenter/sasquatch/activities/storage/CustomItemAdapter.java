/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.models.Document;

import java.util.ArrayList;
import java.util.Map;

public class CustomItemAdapter extends RecyclerView.Adapter<CustomItemAdapter.CustomItemAdapterHolder> {
    private ArrayList<Document<Map>> mList;
    private Context mContext;
    private CustomItemAdapter.OnItemClickListener mListener;

    public CustomItemAdapter(ArrayList<Document<Map>> list, Context context) {
        this.mList = new ArrayList<>(list);
        this.mContext = context;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public CustomItemAdapterHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CustomItemAdapterHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_property, null, false));
    }

    public void setOnItemClickListener(CustomItemAdapter.OnItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomItemAdapterHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(position);
                }
            }
        });
        holder.listItemText.setText(mList.get(position).getId());
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRemoveClick(position);
                }
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

    public void setList(ArrayList<Document<Map>> list) {
        this.mList = list;
    }

    public void removeItem(int position) {
        this.mList.remove(position);
    }

    public String getItem(int position) {
        return mList.get(position).getId();
    }

    public String getDocumentByPosition(int position) {
        return mList.get(position).getId();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onRemoveClick(int position);
    }

    class CustomItemAdapterHolder extends RecyclerView.ViewHolder {

        TextView listItemText;
        ImageButton deleteBtn;

        CustomItemAdapterHolder(@NonNull View itemView) {
            super(itemView);
            listItemText = itemView.findViewById(R.id.property);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }
}
