package avalanche.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import avalanche.analytics.Analytics;
import avalanche.core.Avalanche;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ErrorReporting;
import avalanche.sasquatch.R;
import avalanche.sasquatch.features.TestFeatures;
import avalanche.sasquatch.features.TestFeaturesListAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Avalanche.start(getApplication(), UUIDUtils.randomUUID().toString(), Analytics.class, ErrorReporting.class);
        Avalanche.setLogLevel(Log.VERBOSE);

        TestFeatures.initialize(this);
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(TestFeatures.getAvailableControls()));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }
}
