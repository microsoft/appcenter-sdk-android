package com.microsoft.azure.mobile.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * A service to handle the creation, rotation, and updating of registration tokens.
 */
public class TokenService extends Service {

    private final FirebaseInstanceIdService mFirebaseInstanceIdService;

    public TokenService() {

        /* If Firebase not present, this service would crash, so that's why we wrap it. */
        if (FirebaseUtils.isFirebaseAvailable()) {
            mFirebaseInstanceIdService = new FirebaseInstanceIdService() {

                @Override
                public Context getApplicationContext() {
                    return TokenService.this.getApplicationContext();
                }

                @Override
                public void onTokenRefresh() {

                    /* Firebase presence already tested, avoid try/catch by using API directly. */
                    Push.getInstance().onTokenRefresh(FirebaseInstanceId.getInstance().getToken());
                }
            };
        } else {
            mFirebaseInstanceIdService = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mFirebaseInstanceIdService != null) {
            return mFirebaseInstanceIdService.onBind(intent);
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFirebaseInstanceIdService != null) {
            return mFirebaseInstanceIdService.onStartCommand(intent, flags, startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @VisibleForTesting
    FirebaseInstanceIdService getFirebaseInstanceIdService() {
        return mFirebaseInstanceIdService;
    }
}
