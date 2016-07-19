package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import avalanche.core.Avalanche;
import avalanche.sasquatch.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            CheckBoxPreference preference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.enabled));
            boolean enabled = Avalanche.isEnabled();
            updateSummary(preference, enabled);
            preference.setChecked(enabled);
            preference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = Boolean.parseBoolean(newValue.toString());
            updateSummary(preference, enabled);
            Avalanche.setEnabled(enabled);
            return true;
        }

        private void updateSummary(Preference preference, boolean enabled) {
            if (enabled)
                preference.setSummary(R.string.avalanche_enabled);
            else
                preference.setSummary(R.string.avalanche_disabled);
        }
    }
}
