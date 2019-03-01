package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class AuthenticationProviderActivity extends AppCompatActivity {

    private boolean mUserLeaving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_provider_list);

        /* Populate UI. */
        List<TestFeatures.TestFeatureModel> featureList = new ArrayList<>();
        featureList.add(new TestFeatures.TestFeatureTitle(R.string.msa_title));
        featureList.add(new TestFeatures.TestFeature(R.string.msa_compact_title, R.string.msa_compact_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_COMPACT);
            }
        }));
        featureList.add(new TestFeatures.TestFeature(R.string.msa_delegate_title, R.string.msa_delegate_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_DELEGATE);
            }
        }));

        /* TODO remove reflection once Identity published to jCenter. */
        try {
            final Class<?> identity = Class.forName("com.microsoft.appcenter.identity.Identity");
            featureList.add(new TestFeatures.TestFeature(R.string.b2c_login_title, R.string.b2c_login_description, new View.OnClickListener() {

                /* TODO remove reflection once Identity published to jCenter. Remove this annotation too. */
                @SuppressWarnings("unchecked")
                @Override
                public void onClick(View v) {
                    try {
                        AppCenterFuture<Object> future = (AppCenterFuture<Object>) identity.getMethod("signIn").invoke(null);
                        future.thenAccept(new AppCenterConsumer<Object>() {

                            @Override
                            public void accept(Object signInResult) {
                                try {
                                    Class<?> signInResultClass = signInResult.getClass();
                                    Method getException = signInResultClass.getMethod("getException");
                                    Exception exception = (Exception) getException.invoke(signInResult);
                                    if (exception != null) {
                                        throw exception;
                                    }
                                    Method getUserInformation = signInResultClass.getMethod("getUserInformation");
                                    Object userInformation = getUserInformation.invoke(signInResult);
                                    String accountId = (String) userInformation.getClass().getMethod("getAccountId").invoke(userInformation);
                                    Log.i(LOG_TAG, "Identity.signIn succeeded, accountId=" + accountId);
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "Identity.signIn failed", e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Identity.signIn failed", e);
                    }
                }
            }));
        } catch (ClassNotFoundException ignore) {
        }
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(featureList));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    private void startMSALoginActivity(AuthenticationProvider.Type type) {
        Intent intent = new Intent(getApplication(), MSALoginActivity.class);
        intent.putExtra(AuthenticationProvider.Type.class.getName(), type);
        startActivity(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        mUserLeaving = true;
    }

    @Override
    protected void onRestart() {

        /* When coming back from browser, finish this intermediate menu screen too. */
        super.onRestart();
        if (mUserLeaving) {
            finish();
        }
    }
}
