package com.microsoft.appcenter.distribute.install;

import android.net.Uri;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

public interface ReleaseInstaller {

    @AnyThread
    void install(@NonNull Uri localUri);

    void resume();

    void clear();

    interface Listener {
        void onError(String message);
        void onCancel();
    }
}
