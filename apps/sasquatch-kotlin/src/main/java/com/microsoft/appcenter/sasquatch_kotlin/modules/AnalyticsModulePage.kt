package com.microsoft.appcenter.sasquatch_kotlin.modules

import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.EditTextPreference
import com.microsoft.appcenter.kotlin.Analytics
import com.microsoft.appcenter.sasquatch_kotlin.R

class AnalyticsModulePage : BaseModulePage() {
    override val preferencesResource: Int = R.xml.analytics

    override fun updatePreferences() {
        updateAnalyticsState()
        updateTrackEventName()
        updateTrackEvent()
    }

    private fun updateAnalyticsState() = updatePreference<SwitchPreference>(R.string.analytics_state) {
        isChecked = Analytics.isEnabled()
        onPreferenceChangeListener = onPreferenceChange<Boolean> {
            Analytics.setEnabled(it)
            isChecked = Analytics.isEnabled()
        }
    }

    private fun updateTrackEventName() = updatePreference<EditTextPreference>(R.string.analytics_track_event_name) {
        summary = text
        onPreferenceChangeListener = onPreferenceChange<Boolean> {
            summary = text
        }
    }

    private fun updateTrackEvent() = updateClickPreference(R.string.analytics_track_event) {
        val name = getPreference<EditTextPreference>(R.string.analytics_track_event_name).text
        Analytics.trackEvent(name)
    }
}
