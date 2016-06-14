package com.microsoft.android.avalanchesdktest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.microsoft.android.Avalanche;
import com.microsoft.android.crash.CrashManager;
import com.microsoft.android.crash.CrashManagerListener;
import com.microsoft.android.utils.AvalancheLog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.configure(getApplication(), CrashManager.getInstance().register(this, new CrashManagerListener() {
            @Override
            public boolean shouldAutoUploadCrashes() {
                return true;
            }
        }));

        boolean crashManagerAvailable = Avalanche.isFeatureAvailable(Avalanche.FEATURE_CRASH);
        boolean updateManagerAvailable = Avalanche.isFeatureAvailable(Avalanche.FEATURE_UPDATE);

        AvalancheLog.info("crash available: " + crashManagerAvailable);
        AvalancheLog.info("update available: " + updateManagerAvailable);

        boolean crashManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Avalanche.FEATURE_CRASH);
        boolean updateManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Avalanche.FEATURE_UPDATE);

        AvalancheLog.info("crash enabled: " + crashManagerEnabled);
        AvalancheLog.info("update enabled: " + updateManagerEnabled);
    }
}
