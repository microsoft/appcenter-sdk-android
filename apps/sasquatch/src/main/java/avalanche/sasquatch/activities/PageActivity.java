package avalanche.sasquatch.activities;

import java.util.Map;

import avalanche.analytics.Analytics;

public class PageActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        Analytics.trackPage(name, properties);
    }
}
