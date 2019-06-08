package com.microsoft.appcenter.sasquatch_kotlin.modules

import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.Preference
import com.microsoft.appcenter.kotlin.AppCenter
import com.microsoft.appcenter.sasquatch_kotlin.R

class OverviewModulePage : BaseModulePage() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.overview)
        updateAppCenterState()
        updateSdkVersion()
        updateInstallId()
    }

    private fun updateAppCenterState() = updatePreference<SwitchPreference>(R.string.appcenter_state) {
        isChecked = AppCenter.isEnabled()
        onPreferenceChangeListener = onPreferenceChange<Boolean> {
            AppCenter.setEnabled(it)
            isChecked = AppCenter.isEnabled()
        }
    }

    private fun updateSdkVersion() = updatePreference<Preference>(R.string.appcenter_sdk_version) {
        summary = AppCenter.sdkVersion
    }

    private fun updateInstallId() = updatePreference<Preference>(R.string.appcenter_install_id) {
        summary = AppCenter.getInstallId()?.toString()
    }
}
