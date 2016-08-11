package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import avalanche.analytics.Analytics;
import avalanche.core.Avalanche;
import avalanche.errors.ErrorReporting;
import avalanche.sasquatch.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            final CheckBoxPreference analyticsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.avalanche_analytics_state_key));
            final CheckBoxPreference errorsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.avalanche_errors_state_key));
            initSetting(R.string.avalanche_state_key, Avalanche.isEnabled(), R.string.avalanche_state_summary_enabled, R.string.avalanche_state_summary_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Avalanche.setEnabled(enabled);
                    analyticsEnabledPreference.setChecked(enabled);
                    analyticsEnabledPreference.getOnPreferenceChangeListener().onPreferenceChange(analyticsEnabledPreference, enabled);
                    errorsEnabledPreference.setChecked(enabled);
                    errorsEnabledPreference.getOnPreferenceChangeListener().onPreferenceChange(errorsEnabledPreference, enabled);
                }
            });
            initSetting(R.string.avalanche_analytics_state_key, Analytics.isEnabled(), R.string.avalanche_analytics_state_summary_enabled, R.string.avalanche_analytics_state_summary_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Analytics.setEnabled(enabled);
                }
            });
            initSetting(R.string.avalanche_errors_state_key, ErrorReporting.isEnabled(), R.string.avalanche_errors_state_summary_enabled, R.string.avalanche_errors_state_summary_disabled, new SetEnabled() {
                @Override
                public void setEnabled(boolean enabled) {
                    ErrorReporting.setEnabled(enabled);
                }
            });
            initSetting(R.string.avalanche_auto_page_tracking_key, Analytics.isAutoPageTrackingEnabled(), R.string.avalanche_auto_page_tracking_enabled, R.string.avalanche_auto_page_tracking_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Analytics.setAutoPageTrackingEnabled(enabled);
                }
            });
        }

        private void initSetting(int key, boolean enabled, final int enabledSummary, final int disabledSummary, final SetEnabled setEnabled) {
            CheckBoxPreference preference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(key));
            updateSummary(preference, enabled, enabledSummary, disabledSummary);
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (Boolean) newValue;
                    setEnabled.setEnabled(enabled);
                    updateSummary(preference, enabled, enabledSummary, disabledSummary);
                    return true;
                }
            });
            preference.setChecked(enabled);
        }

        private void updateSummary(Preference preference, boolean enabled, int enabledSummary, int disabledSummary) {
            if (enabled)
                preference.setSummary(enabledSummary);
            else
                preference.setSummary(disabledSummary);
        }

        private interface SetEnabled {
            void setEnabled(boolean enabled);
        }
    }
}
