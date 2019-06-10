package com.microsoft.appcenter.sasquatch_kotlin.modules

import android.support.v14.preference.SwitchPreference
import com.microsoft.appcenter.kotlin.Crashes
import com.microsoft.appcenter.sasquatch_kotlin.R

class CrashesModulePage : BaseModulePage() {
    override val preferencesResource: Int = R.xml.crashes

    override fun updatePreferences() {
        updateCrashesState()
        updateGenerateTestCrash()
    }

    private fun updateCrashesState() = updatePreference<SwitchPreference>(R.string.crashes_state) {
        isChecked = Crashes.isEnabled()
        onPreferenceChangeListener = onPreferenceChange<Boolean> {
            Crashes.setEnabled(it)
            isChecked = Crashes.isEnabled()
        }
    }

    private fun updateGenerateTestCrash() = updateClickPreference(R.string.crashes_generate_test_crash) {
        Crashes.generateTestCrash()
    }
}
