package com.microsoft.appcenter.sasquatch.activities;

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

public class AppDocumentListAdapter extends RecyclerView.Adapter<AppDocumentListAdapter.AppDocumentListHolder> {
    private Context mContext;
    private List<Document<TestDocument>> mList;
    private OnItemClickListener mListener;

    AppDocumentListAdapter(Context context, List<Document<TestDocument>> list) {
        this.mContext = context;
        this.mList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public AppDocumentListHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new AppDocumentListHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_app, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AppDocumentListHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.titleFile.setText(mList.get(position).getId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void upload(List<Document<TestDocument>> list) {
        mList.addAll(list);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    String getDocumentByPosition(int position) {
        return mList.get(position).getId();
    }

    String getItem(int position) {
        return mList.get(position).getId();
    }

    class AppDocumentListHolder extends RecyclerView.ViewHolder {

        TextView titleFile;

        AppDocumentListHolder(@NonNull View itemView) {
            super(itemView);
            titleFile = itemView.findViewById(R.id.property_app);
        }
    }
}
