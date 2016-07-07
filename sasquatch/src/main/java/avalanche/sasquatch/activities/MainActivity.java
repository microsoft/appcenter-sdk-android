package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import java.util.UUID;

import avalanche.base.Avalanche;
import avalanche.base.utils.AvalancheLog;
import avalanche.crash.Crashes;
import avalanche.sasquatch.R;
import avalanche.sasquatch.features.TestFeatures;
import avalanche.sasquatch.features.TestFeaturesListAdapter;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.useFeatures(getApplication(), UUID.randomUUID().toString(), Crashes.class);

        AvalancheLog.setLogLevel(Log.VERBOSE);

        boolean crashManagerAvailable = Avalanche.isFeatureAvailable(Crashes.class.getName());

        AvalancheLog.info("crash available: " + crashManagerAvailable);

        boolean crashManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Crashes.class.getName());

        AvalancheLog.info("crash enabled: " + crashManagerEnabled);

        TestFeatures.initialize(this);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(new TestFeaturesListAdapter(TestFeatures.getAvailableControls()));
        mListView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }
}
