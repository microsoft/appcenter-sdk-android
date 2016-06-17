package com.microsoft.android.sasquatch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.microsoft.android.Avalanche;
import com.microsoft.android.crash.CrashManager;
import com.microsoft.android.utils.AvalancheLog;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mCrashButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.configure(getApplication());

        boolean crashManagerAvailable = Avalanche.isFeatureAvailable(Avalanche.FEATURE_CRASH);
        boolean updateManagerAvailable = Avalanche.isFeatureAvailable(Avalanche.FEATURE_UPDATE);

        AvalancheLog.info("crash available: " + crashManagerAvailable);
        AvalancheLog.info("update available: " + updateManagerAvailable);

        boolean crashManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Avalanche.FEATURE_CRASH);
        boolean updateManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Avalanche.FEATURE_UPDATE);

        AvalancheLog.info("crash enabled: " + crashManagerEnabled);
        AvalancheLog.info("update enabled: " + updateManagerEnabled);

        mCrashButton = (Button) findViewById(R.id.button_crash);
        mCrashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> fakeList = new ArrayList<>();
                fakeList.get(1000);
            }
        });
    }
}
