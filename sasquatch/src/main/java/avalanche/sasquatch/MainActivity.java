package avalanche.sasquatch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import avalanche.base.AvalancheHub;
import avalanche.base.utils.AvalancheLog;

public class MainActivity extends AppCompatActivity {

    private Button mCrashButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AvalancheHub.use(getApplication());

        AvalancheLog.setLogLevel(Log.INFO);

        boolean crashManagerAvailable = AvalancheHub.isFeatureAvailable(AvalancheHub.FEATURE_CRASH);

        AvalancheLog.info("crash available: " + crashManagerAvailable);

        boolean crashManagerEnabled = AvalancheHub.getSharedInstance().isFeatureEnabled(AvalancheHub.FEATURE_CRASH);

        AvalancheLog.info("crash enabled: " + crashManagerEnabled);

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
