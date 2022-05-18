package com.microsoft.appcenter.distribute.install;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface ReleaseInstaller {

    @WorkerThread
    void install(@NonNull Uri localUri);

    void resume();

    void clear();

    interface Listener {
        void onError(String message);
        void onCancel();
    }
}
