/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.ArrayList;
import java.util.Map;

public class CustomItemAdapter extends RecyclerView.Adapter<CustomItemAdapter.CustomItemAdapterHolder> {

    private ArrayList<DocumentWrapper<Map>> mList;

    private final Context mContext;

    private CustomItemAdapter.OnItemClickListener mListener;

    public CustomItemAdapter(ArrayList<DocumentWrapper<Map>> list, Context context) {
        mList = new ArrayList<>(list);
        mContext = context;
    }

    @NonNull
    @Override
    public CustomItemAdapterHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CustomItemAdapterHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_property, viewGroup, false));
    }

    public void setOnItemClickListener(CustomItemAdapter.OnItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull final CustomItemAdapterHolder holder, int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(holder.getAdapterPosition());
                }
            }
        });
        holder.documentIdTextView.setText(mList.get(position).getId());
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRemoveClick(holder.getAdapterPosition());
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

    public void setList(ArrayList<DocumentWrapper<Map>> list) {
        mList = list;
    }

    public void removeItem(int position) {
        mList.remove(position);
    }

    public String getItem(int position) {
        return mList.get(position).getId();
    }

    public DocumentWrapper<Map> getDocument(int position) {
        return mList.get(position);
    }

    public interface OnItemClickListener {

        void onItemClick(int position);

        void onRemoveClick(int position);
    }

    class CustomItemAdapterHolder extends RecyclerView.ViewHolder {

        final TextView documentIdTextView;

        final ImageButton deleteButton;

        CustomItemAdapterHolder(@NonNull View itemView) {
            super(itemView);
            documentIdTextView = itemView.findViewById(R.id.property);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
