package com.microsoft.azure.mobile.sasquatch.activities;

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
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.analytics.AnalyticsPrivateHelper;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.push.Push;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.UUID;

import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.APP_SECRET_KEY;
import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.PUSH_SENDER_ID_KEY;
import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.SERVER_URL_KEY;

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
            final CheckBoxPreference analyticsEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.mobile_center_analytics_state_key));
            final CheckBoxPreference crashesEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.mobile_center_crashes_state_key));
            final CheckBoxPreference pushEnabledPreference = (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.mobile_center_push_state_key));
            initCheckBoxSetting(R.string.mobile_center_state_key, MobileCenter.isEnabled(), R.string.mobile_center_state_summary_enabled, R.string.mobile_center_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    MobileCenter.setEnabled(enabled);
                    analyticsEnabledPreference.setChecked(Analytics.isEnabled());
                    crashesEnabledPreference.setChecked(Crashes.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return MobileCenter.isEnabled();
                }
            });
            initCheckBoxSetting(R.string.mobile_center_analytics_state_key, Analytics.isEnabled(), R.string.mobile_center_analytics_state_summary_enabled, R.string.mobile_center_analytics_state_summary_disabled, new HasEnabled() {

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
            initCheckBoxSetting(R.string.mobile_center_crashes_state_key, Crashes.isEnabled(), R.string.mobile_center_crashes_state_summary_enabled, R.string.mobile_center_crashes_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Crashes.setEnabled(enabled);
                    crashesEnabledPreference.setChecked(Crashes.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return Crashes.isEnabled();
                }
            });
            initCheckBoxSetting(R.string.mobile_center_push_state_key, Push.isEnabled(), R.string.mobile_center_push_state_summary_enabled, R.string.mobile_center_push_state_summary_disabled, new HasEnabled() {

                @Override
                public void setEnabled(boolean enabled) {
                    Push.setEnabled(enabled);
                    pushEnabledPreference.setChecked(Push.isEnabled());
                }

                @Override
                public boolean isEnabled() {
                    return Push.isEnabled();
                }
            });
            final String defaultPushSenderId = getString(R.string.push_sender_id);
            initClickableSetting(R.string.mobile_center_push_sender_id_key, MainActivity.sSharedPreferences.getString(PUSH_SENDER_ID_KEY, defaultPushSenderId), new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(MainActivity.sSharedPreferences.getString(PUSH_SENDER_ID_KEY, defaultPushSenderId));

                    new AlertDialog.Builder(getActivity()).setTitle(R.string.mobile_center_push_sender_id_title).setView(input)
                            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String url = input.getText().toString();
                                    setKeyValue(PUSH_SENDER_ID_KEY, url);
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(PUSH_SENDER_ID_KEY, defaultPushSenderId));
                                }
                            })
                            .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setKeyValue(APP_SECRET_KEY, defaultPushSenderId);
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(PUSH_SENDER_ID_KEY, defaultPushSenderId));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
                }
            });
            initCheckBoxSetting(R.string.mobile_center_auto_page_tracking_key, AnalyticsPrivateHelper.isAutoPageTrackingEnabled(), R.string.mobile_center_auto_page_tracking_enabled, R.string.mobile_center_auto_page_tracking_disabled, new HasEnabled() {

                @Override
                public boolean isEnabled() {
                    return AnalyticsPrivateHelper.isAutoPageTrackingEnabled();
                }

                @Override
                public void setEnabled(boolean enabled) {
                    AnalyticsPrivateHelper.setAutoPageTrackingEnabled(enabled);
                }
            });
            initClickableSetting(R.string.install_id_key, String.valueOf(MobileCenter.getInstallId()), new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(String.valueOf(MobileCenter.getInstallId()));

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
                                    preference.setSummary(String.valueOf(MobileCenter.getInstallId()));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
                }
            });
            initClickableSetting(R.string.app_secret_key, MainActivity.sSharedPreferences.getString(APP_SECRET_KEY, null), new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(MainActivity.sSharedPreferences.getString(APP_SECRET_KEY, null));

                    new AlertDialog.Builder(getActivity()).setTitle(R.string.app_secret_title).setView(input)
                            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String appSecret = input.getText().toString();
                                    if (!TextUtils.isEmpty(appSecret)) {
                                        setKeyValue(APP_SECRET_KEY, appSecret);
                                        Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.app_secret_changed_format), appSecret), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), R.string.app_secret_invalid, Toast.LENGTH_SHORT).show();
                                    }
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(APP_SECRET_KEY, null));
                                }
                            })
                            .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setKeyValue(APP_SECRET_KEY, MainActivity.APP_SECRET);
                                    Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.app_secret_changed_format), MainActivity.APP_SECRET), Toast.LENGTH_SHORT).show();
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(APP_SECRET_KEY, null));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
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
            initClickableSetting(R.string.server_url_key, MainActivity.sSharedPreferences.getString(SERVER_URL_KEY, getString(R.string.server_url_production)), new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(MainActivity.sSharedPreferences.getString(SERVER_URL_KEY, null));
                    input.setHint(R.string.server_url_production);

                    new AlertDialog.Builder(getActivity()).setTitle(R.string.server_url_title).setView(input)
                            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Patterns.WEB_URL.matcher(input.getText().toString()).matches()) {
                                        String url = input.getText().toString();
                                        setKeyValue(SERVER_URL_KEY, url);
                                        Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.server_url_changed_format), url), Toast.LENGTH_SHORT).show();
                                    } else if (input.getText().toString().isEmpty()) {
                                        setProductionUrl();
                                    } else {
                                        Toast.makeText(getActivity(), R.string.server_url_invalid, Toast.LENGTH_SHORT).show();
                                    }
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(SERVER_URL_KEY, getString(R.string.server_url_production)));
                                }
                            })
                            .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setProductionUrl();
                                    preference.setSummary(MainActivity.sSharedPreferences.getString(SERVER_URL_KEY, getString(R.string.server_url_production)));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                    return true;
                }

                private void setProductionUrl() {
                    setKeyValue(SERVER_URL_KEY, null);
                    Toast.makeText(getActivity(), R.string.server_url_production, Toast.LENGTH_SHORT).show();
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
            initClickableSetting(key, null, listener);
        }

        @SuppressWarnings("SameParameterValue")
        private void initClickableSetting(int key, String summary, Preference.OnPreferenceClickListener clickListener) {
            Preference preference = getPreferenceManager().findPreference(getString(key));
            preference.setOnPreferenceClickListener(clickListener);
            if (summary != null) {
                preference.setSummary(summary);
            }
        }

        private void updateSummary(Preference preference, boolean enabled, int enabledSummary, int disabledSummary) {
            if (enabled)
                preference.setSummary(enabledSummary);
            else
                preference.setSummary(disabledSummary);
        }

        private void setKeyValue(String key, String value) {
            SharedPreferences.Editor editor = MainActivity.sSharedPreferences.edit();
            if (value == null)
                editor.remove(key);
            else
                editor.putString(key, value);
            editor.apply();
        }

        private interface HasEnabled {
            boolean isEnabled();

            void setEnabled(boolean enabled);
        }
    }
}
