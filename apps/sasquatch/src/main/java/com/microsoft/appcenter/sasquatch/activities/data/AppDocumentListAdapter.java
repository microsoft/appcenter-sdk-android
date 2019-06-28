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
import android.widget.TextView;

import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppDocumentListAdapter extends RecyclerView.Adapter<AppDocumentListAdapter.AppDocumentListHolder> {

    private final Context mContext;

    private final List<DocumentWrapper<Map>> mList;

    private OnItemClickListener mListener;

    public AppDocumentListAdapter(Context context, List<DocumentWrapper<Map>> list) {
        mContext = context;
        mList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public AppDocumentListHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new AppDocumentListHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_app, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final AppDocumentListHolder holder, int position) {
        holder.titleFile.setText(mList.get(position).getId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void upload(List<DocumentWrapper<Map>> list) {
        if (list != null) {
            mList.clear();
            mList.addAll(list);
        }
    }

    public interface OnItemClickListener {

        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public DocumentWrapper<Map> getDocument(int position) {
        return mList.get(position);
    }

    class AppDocumentListHolder extends RecyclerView.ViewHolder {

        final TextView titleFile;

        AppDocumentListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.property_app);
        }
    }
}
