/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

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
import com.microsoft.appcenter.sasquatch.activities.StorageActivity;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.activities.StorageActivity.CACHED_PREFIX;

public class CustomItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int LAYOUT_HEADER = 0;
    private static final int LAYOUT_CHILD = 1;

    private ArrayList<ListItem> mList;
    private Context mContext;
    private AppDocumentListAdapter.OnItemClickListener listener;

    private int mRemotePosition = 0;

    public CustomItemAdapter(ArrayList<String> list, Context context) {
        mContext = context;
        mList = new ArrayList<>();
        if (list.size() > 0) {
            sortAndUploadDocumentList(list);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mList.get(position).isHeader()) {
            return LAYOUT_HEADER;
        } else {
            return LAYOUT_CHILD;
        }
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder holder;
        if (viewType == LAYOUT_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_title, viewGroup, false);
            holder = new UserDocumentHeaderListHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_view_property, null, false);
            holder = new UserDocumentListHolder(view);
        }
        return holder;
    }

    public void setOnItemClickListener(AppDocumentListAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        if (holder.getItemViewType() == LAYOUT_HEADER) {
            UserDocumentHeaderListHolder vaultItemHolder = (UserDocumentHeaderListHolder) holder;
            vaultItemHolder.titleFile.setText(mList.get(position).getName());
        } else {
            UserDocumentListHolder vaultItemHolder = (UserDocumentListHolder) holder;
            vaultItemHolder.listItemText.setText(mList.get(position).getName());
            vaultItemHolder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Storage.delete(Constants.USER, StorageActivity.sUserDocumentList.get(position));
                    mList.remove(position);
                    notifyDataSetChanged();
                }
            });
            vaultItemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onItemClick(position);
                }
            });
        }
    }

    private void sortAndUploadDocumentList(ArrayList<String> documentList) {
        List<ListItem> cachedDocuments = new ArrayList<>();
        List<ListItem> remoteDocuments = new ArrayList<>();
        for (String doc : documentList) {
            ChildUserListItem listItem = new ChildUserListItem();
            listItem.setChild(doc);
            if (doc.startsWith(CACHED_PREFIX)) {
                cachedDocuments.add(listItem);
            } else {
                remoteDocuments.add(listItem);
            }
        }
        if (mRemotePosition > 0) {
            mList.addAll(mRemotePosition - 1, cachedDocuments);
            mList.addAll(remoteDocuments);
            mRemotePosition += cachedDocuments.size();
            return;
        }
        mList = new ArrayList<>();
        HeaderListItem cachedHeader = new HeaderListItem();
        cachedHeader.setHeader(mContext.getString(R.string.documents_from_cache_header));
        HeaderListItem remoteHeader = new HeaderListItem();
        remoteHeader.setHeader(mContext.getString(R.string.documents_from_remote_header));
        mList.add(cachedHeader);
        mList.addAll(cachedDocuments);
        mRemotePosition = cachedDocuments.size() + 2;
        mList.add(remoteHeader);
        mList.addAll(remoteDocuments);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    class UserDocumentListHolder extends RecyclerView.ViewHolder {

        TextView listItemText;
        ImageButton deleteBtn;

        UserDocumentListHolder(@NonNull View itemView) {
            super(itemView);
            listItemText = itemView.findViewById(R.id.property);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }

    class UserDocumentHeaderListHolder extends RecyclerView.ViewHolder {

        TextView titleFile;

        UserDocumentHeaderListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.title);
        }
    }
}
