package com.microsoft.appcenter.distribute.download;

import android.content.Context;

import com.microsoft.appcenter.distribute.ReleaseDetails;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    private Listener mListener;

    HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails) {
        // todo download file.
    }

    @Override
    public void delete() {
        // todo delete file.
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }
}
