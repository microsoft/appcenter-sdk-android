package com.microsoft.sonoma.sasquatch.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.sonoma.analytics.Analytics;
import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.utils.PrefStorageConstants;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.crashes.Crashes;
import com.microsoft.sonoma.sasquatch.R;

import java.util.UUID;

import static com.microsoft.sonoma.sasquatch.activities.MainActivity.APP_SECRET_KEY;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private static final String UUID_FORMAT_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            final CheckBoxPreference analyticsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.sonoma_analytics_state_key));
            final CheckBoxPreference crashesEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.sonoma_crashes_state_key));
            initCheckBoxSetting(R.string.sonoma_state_key, Sonoma.isEnabled(), R.string.sonoma_state_summary_enabled, R.string.sonoma_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Sonoma.setEnabled(enabled);
                    analyticsEnabledPreference.setChecked(Analytics.isEnabled());
                    crashesEnabledPreference.setChecked(Crashes.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return Sonoma.isEnabled();
                }
            });
            initCheckBoxSetting(R.string.sonoma_analytics_state_key, Analytics.isEnabled(), R.string.sonoma_analytics_state_summary_enabled, R.string.sonoma_analytics_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Analytics.setEnabled(enabled);
                    analyticsEnabledPreference.setChecked(Analytics.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return Analytics.isEnabled();
                }
            });
            initCheckBoxSetting(R.string.sonoma_crashes_state_key, Crashes.isEnabled(), R.string.sonoma_crashes_state_summary_enabled, R.string.sonoma_crashes_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Crashes.setEnabled(enabled);
                    crashesEnabledPreference.setChecked(Analytics.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return Crashes.isEnabled();
                }
            });
            initCheckBoxSetting(R.string.sonoma_auto_page_tracking_key, Analytics.isAutoPageTrackingEnabled(), R.string.sonoma_auto_page_tracking_enabled, R.string.sonoma_auto_page_tracking_disabled, new HasEnabled() {

                @Override
                public boolean isEnabled() {
                    return Analytics.isAutoPageTrackingEnabled();
                }

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
            initClickableSetting(R.string.install_id_key, new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(String.valueOf(Sonoma.getInstallId()));

                    new AlertDialog.Builder(getActivity()).setTitle(R.string.install_id_title).setView(input)
                            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (input.getText().toString().matches(UUID_FORMAT_REGEX)) {
                                        UUID uuid = UUID.fromString(input.getText().toString());
                                        StorageHelper.PreferencesStorage.putString(PrefStorageConstants.KEY_INSTALL_ID, uuid.toString());
                                        Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.install_id_changed_format), uuid.toString()), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), R.string.install_id_invalid, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
                }
            });
            initClickableSetting(R.string.app_secret_key, new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(MainActivity.sSharedPreferences.getString(APP_SECRET_KEY, null));

                    new AlertDialog.Builder(getActivity()).setTitle(R.string.app_secret_title).setView(input)
                            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (input.getText().toString().matches(UUID_FORMAT_REGEX)) {
                                        UUID uuid = UUID.fromString(input.getText().toString());
                                        setAppSecret(uuid);
                                        Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.app_secret_changed_format), uuid.toString()), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), R.string.app_secret_invalid, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setAppSecret(UUID.fromString(MainActivity.APP_SECRET));
                                    Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.app_secret_changed_format), MainActivity.APP_SECRET), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
                }

                private void setAppSecret(UUID uuid) {
                    SharedPreferences.Editor editor = MainActivity.sSharedPreferences.edit();
                    editor.putString(APP_SECRET_KEY, uuid.toString());
                    editor.apply();
                }
            });
        }

        private void initCheckBoxSetting(int key, boolean enabled, final int enabledSummary, final int disabledSummary, final HasEnabled hasEnabled) {
            CheckBoxPreference preference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(key));
            updateSummary(preference, enabled, enabledSummary, disabledSummary);
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (Boolean) newValue;
                    hasEnabled.setEnabled(enabled);
                    if (hasEnabled.isEnabled() == enabled) {
                        updateSummary(preference, enabled, enabledSummary, disabledSummary);
                        return true;
                    }

                    return false;
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

        private interface HasEnabled {
            boolean isEnabled();

            void setEnabled(boolean enabled);
        }
    }
}
