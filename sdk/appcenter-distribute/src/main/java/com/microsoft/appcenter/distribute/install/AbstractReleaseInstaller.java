package com.microsoft.appcenter.distribute.install;

import android.content.Context;

public abstract class AbstractReleaseInstaller implements ReleaseInstaller {
    protected final Context mContext;
    protected final Listener mListener;

    protected AbstractReleaseInstaller(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }
}
