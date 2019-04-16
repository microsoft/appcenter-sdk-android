package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.models.Document;

import java.util.ArrayList;
import java.util.List;

public class AppDocumentListAdapter extends RecyclerView.Adapter<AppDocumentListAdapter.AppDocumentListHolder> {
    private Context mContext;
    private List<Document<TestDocument>> mList;
    private OnItemClickListener mListener;

    public AppDocumentListAdapter(Context context, List<Document<TestDocument>> list) {
        this.mContext = context;
        this.mList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public AppDocumentListHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new AppDocumentListHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_app, null, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AppDocumentListHolder holder, final int position) {
        holder.titleFile.setText(mList.get(position).getId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onItemClick(position);
            }
        });
    }

    public String getItem(int position) {
        return mList.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void upload(List<Document<TestDocument>> list) {
        mList.addAll(list);
    }

    public String getDocumentByPosition(int position) {
        TestDocument document = mList.get(position).getDocument();
        return document == null ? "{}" : Utils.getGson().toJson(document);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class AppDocumentListHolder extends RecyclerView.ViewHolder {

        public TextView titleFile;

        public AppDocumentListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.property_app);
        }
    }
}
