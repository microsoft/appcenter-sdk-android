package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import avalanche.analytics.Analytics;
import avalanche.core.Avalanche;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.UUIDUtils;
import avalanche.crash.Crashes;
import avalanche.sasquatch.R;
import avalanche.sasquatch.features.TestFeatures;
import avalanche.sasquatch.features.TestFeaturesListAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.useFeatures(getApplication(), UUIDUtils.randomUUID().toString(), Analytics.class, Crashes.class);

        AvalancheLog.setLogLevel(Log.VERBOSE);

        TestFeatures.initialize(this);
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(TestFeatures.getAvailableControls()));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }
}
