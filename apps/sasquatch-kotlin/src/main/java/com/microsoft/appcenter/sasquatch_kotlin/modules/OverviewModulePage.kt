package com.microsoft.appcenter.sasquatch_kotlin.modules

import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.Preference
import com.microsoft.appcenter.kotlin.AppCenter
import com.microsoft.appcenter.kotlin.Auth
import com.microsoft.appcenter.sasquatch_kotlin.R
import org.jetbrains.anko.support.v4.toast

class OverviewModulePage : BaseModulePage() {
    override val preferencesResource: Int = R.xml.overview

    override fun updatePreferences() {
        updateAppCenterState()
        updateSdkVersion()
        updateInstallId()

        updateAuthState()
        updateSignIn()
        updateSignOut()
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

    private fun updateAuthState() = updatePreference<SwitchPreference>(R.string.auth_state) {
        isChecked = Auth.isEnabled()
        onPreferenceChangeListener = onPreferenceChange<Boolean> {
            Auth.setEnabled(it)
            isChecked = Auth.isEnabled()
        }
    }

    private fun updateSignIn() = updateClickPreference(R.string.auth_sign_in) {
        try {
            val userInformation = Auth.signIn()
            toast("User sign-in succeeded. Account Id: ${userInformation.accountId}")
        } catch (exception: Exception) {
            toast("User sign-in failed. Exception: ${exception.message}")
        }
    }

    private fun updateSignOut() = updateClickPreference(R.string.auth_sign_out) {
        Auth.signOut()
    }
}
