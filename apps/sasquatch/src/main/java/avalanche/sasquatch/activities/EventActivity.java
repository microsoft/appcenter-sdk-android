package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import avalanche.analytics.Analytics;
import avalanche.sasquatch.R;

public class EventActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_event);
    }

    public void sendEvent(@SuppressWarnings("UnusedParameters") View view) {
        String name = ((TextView) findViewById(R.id.event_name)).getText().toString();
        Analytics.trackEvent(name, null);
    }
}
