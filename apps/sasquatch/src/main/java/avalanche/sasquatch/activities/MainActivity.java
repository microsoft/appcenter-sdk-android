package avalanche.sasquatch.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import avalanche.analytics.Analytics;
import avalanche.core.Avalanche;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.AbstractErrorReportingListener;
import avalanche.errors.ErrorReporting;
import avalanche.errors.model.ErrorReport;
import avalanche.sasquatch.R;
import avalanche.sasquatch.features.TestFeatures;
import avalanche.sasquatch.features.TestFeaturesListAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ErrorReporting.setListener(new AbstractErrorReportingListener() {
            @Override
            public boolean shouldAwaitUserConfirmation() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setTitle(R.string.crash_confirmation_dialog_title)
                        .setMessage(R.string.crash_confirmation_dialog_message)
                        .setPositiveButton(R.string.crash_confirmation_dialog_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ErrorReporting.notifyUserConfirmation(ErrorReporting.SEND);
                            }
                        })
                        .setNegativeButton(R.string.crash_confirmation_dialog_not_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ErrorReporting.notifyUserConfirmation(ErrorReporting.DONT_SEND);
                            }
                        })
                        .setNeutralButton(R.string.crash_confirmation_dialog_always_send_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ErrorReporting.notifyUserConfirmation(ErrorReporting.ALWAYS_SEND);
                            }
                        });
                builder.create().show();
                return true;
            }

            @Override
            public void onSendingFailed(ErrorReport errorReport, Exception e) {
                Toast.makeText(MainActivity.this, R.string.crash_sent_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSendingSucceeded(ErrorReport errorReport) {
                String message = String.format("%s\nCrash ID: %s\nThrowable: %s", R.string.crash_sent_succeeded, errorReport.getId(), errorReport.getThrowable().toString());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        Avalanche.setLogLevel(Log.VERBOSE);
        Avalanche.start(getApplication(), UUIDUtils.randomUUID().toString(), Analytics.class, ErrorReporting.class);

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
