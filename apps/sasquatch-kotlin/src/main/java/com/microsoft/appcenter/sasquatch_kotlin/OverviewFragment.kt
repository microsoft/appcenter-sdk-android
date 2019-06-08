package com.microsoft.appcenter.sasquatch_kotlin

import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.microsoft.appcenter.kotlin.AppCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverviewFragment : PreferenceFragmentCompat() {
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.overview)
        updateAppCenterState()
        updateInstallId()
    }

    private fun updateAppCenterState() =
            with(preferenceManager.findPreference("appcenter_enabled") as SwitchPreference) {
                uiScope.launch {
                    isChecked = AppCenter.isEnabled()
                    onPreferenceChangeListener = onPreferenceChange<Boolean> {
                        AppCenter.setEnabled(it)
                        isChecked = AppCenter.isEnabled()
                    }
                }
            }

    private fun updateInstallId() =
            with(preferenceManager.findPreference("install_id")) {
                uiScope.launch {
                    summary = AppCenter.getInstallId()?.toString()
                }
            }

    private inline fun <reified T> onPreferenceChange(crossinline callback: suspend (value: T) -> Unit) =
            Preference.OnPreferenceChangeListener { _, newValue ->
                uiScope.launch {
                    callback(newValue as T)
                }
                true
            }
}
