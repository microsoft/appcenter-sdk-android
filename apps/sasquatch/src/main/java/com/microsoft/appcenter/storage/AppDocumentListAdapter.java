package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.models.Document;

import java.util.ArrayList;
import java.util.List;

public class AppDocumentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int LAYOUT_HEADER = 0;
    private static final int LAYOUT_CHILD = 1;

    private Context mContext;
    private List<ListItem> mList;
    private OnItemClickListener listener;

    private int mRemotePosition = 0;

    public AppDocumentListAdapter(Context context, List<Document<TestDocument>> list) {
        this.mContext = context;
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder holder;
        if (viewType == LAYOUT_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_title, viewGroup, false);
            holder = new AppDocumentHeaderListHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_view_app, viewGroup, false);
            holder = new AppDocumentListHolder(view);
        }
        return holder;
    }

    private void sortAndUploadDocumentList(List<Document<TestDocument>> documentList) {
        List<ListItem> cachedDocuments = new ArrayList<>();
        List<ListItem> remoteDocuments = new ArrayList<>();
        for (Document<TestDocument> doc : documentList) {
            ChildListItem listItem = new ChildListItem();
            listItem.setChild(doc);
            if (doc.isFromCache()) {
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        if (holder.getItemViewType() == LAYOUT_HEADER) {
            AppDocumentHeaderListHolder vaultItemHolder = (AppDocumentHeaderListHolder) holder;
            vaultItemHolder.titleFile.setText(mList.get(position).getName());
        } else {
            AppDocumentListHolder vaultItemHolder = (AppDocumentListHolder) holder;
            vaultItemHolder.titleFile.setText(mList.get(position).getName());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onItemClick(position);
                }
            });
        }
    }

    public String getItem(int position) {
        return mList.get(position).getName();
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void upload(List<Document<TestDocument>> list) {
        sortAndUploadDocumentList(list);
    }

    public String getDocumentByPosition(int position) {
        return mList.get(position).getItem();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    class AppDocumentListHolder extends RecyclerView.ViewHolder {

        TextView titleFile;

        AppDocumentListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.property_app);
        }
    }

    class AppDocumentHeaderListHolder extends RecyclerView.ViewHolder {

        TextView titleFile;

        AppDocumentHeaderListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.title);
        }
    }
}
