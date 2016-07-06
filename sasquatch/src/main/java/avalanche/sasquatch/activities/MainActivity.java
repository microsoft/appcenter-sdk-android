package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import avalanche.base.Avalanche;
import avalanche.base.utils.AvalancheLog;
import avalanche.sasquatch.R;
import avalanche.sasquatch.features.TestFeatures;
import avalanche.sasquatch.features.TestFeaturesListAdapter;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.useFeatures(getApplication());

        AvalancheLog.setLogLevel(Log.VERBOSE);

        boolean crashManagerAvailable = Avalanche.isFeatureAvailable(Avalanche.FEATURE_CRASH);

        AvalancheLog.info("crash available: " + crashManagerAvailable);

        boolean crashManagerEnabled = Avalanche.getSharedInstance().isFeatureEnabled(Avalanche.FEATURE_CRASH);

        AvalancheLog.info("crash enabled: " + crashManagerEnabled);

        TestFeatures.initialize(this);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(new TestFeaturesListAdapter(TestFeatures.getAvailableControls()));
        mListView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }
}
