package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.microsoft.appcenter.sasquatch.R;

/**
 * TODO: Remove this once new APIs available in jCenter.
 */
public class MicrosoftAuthenticationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msa_auth);
    }

    public void onLoginClick(View view) {
    }

    public void onRefreshClick(View view) {
    }
}
