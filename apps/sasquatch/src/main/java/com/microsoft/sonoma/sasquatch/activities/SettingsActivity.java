package com.microsoft.sonoma.sasquatch.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.microsoft.sonoma.analytics.Analytics;
import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.crashes.Crashes;
import com.microsoft.sonoma.sasquatch.R;

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
            final CheckBoxPreference analyticsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.sonoma_analytics_state_key));
            final CheckBoxPreference errorsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.sonoma_errors_state_key));
            initCheckBoxSetting(R.string.sonoma_state_key, Sonoma.isEnabled(), R.string.sonoma_state_summary_enabled, R.string.sonoma_state_summary_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Sonoma.setEnabled(enabled);
                    analyticsEnabledPreference.setChecked(enabled);
                    errorsEnabledPreference.setChecked(enabled);
                }
            });
            initCheckBoxSetting(R.string.sonoma_analytics_state_key, Analytics.isEnabled(), R.string.sonoma_analytics_state_summary_enabled, R.string.sonoma_analytics_state_summary_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Analytics.setEnabled(enabled);
                }
            });
            initCheckBoxSetting(R.string.sonoma_errors_state_key, Crashes.isEnabled(), R.string.sonoma_errors_state_summary_enabled, R.string.sonoma_errors_state_summary_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Crashes.setEnabled(enabled);
                }
            });
            initCheckBoxSetting(R.string.sonoma_auto_page_tracking_key, Analytics.isAutoPageTrackingEnabled(), R.string.sonoma_auto_page_tracking_enabled, R.string.sonoma_auto_page_tracking_disabled, new SetEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Analytics.setAutoPageTrackingEnabled(enabled);
                }
            });
            initClickableSetting(R.string.clear_crash_user_confirmation_key, new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    StorageHelper.PreferencesStorage.remove(Crashes.PREF_KEY_ALWAYS_SEND);
                    Toast.makeText(getActivity(), R.string.clear_crash_user_confirmation_toast, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        private void initCheckBoxSetting(int key, boolean enabled, final int enabledSummary, final int disabledSummary, final SetEnabled setEnabled) {
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

        @SuppressWarnings("SameParameterValue")
        private void initClickableSetting(int key, Preference.OnPreferenceClickListener listener) {
            Preference preference = getPreferenceManager().findPreference(getString(key));
            preference.setOnPreferenceClickListener(listener);
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
