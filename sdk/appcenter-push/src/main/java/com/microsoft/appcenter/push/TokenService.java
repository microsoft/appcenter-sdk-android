package com.microsoft.appcenter.push;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * A service to handle the creation, rotation, and updating of registration tokens.
 */
public class TokenService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        Push.getInstance().onTokenRefresh(FirebaseInstanceId.getInstance().getToken());
    }
}
