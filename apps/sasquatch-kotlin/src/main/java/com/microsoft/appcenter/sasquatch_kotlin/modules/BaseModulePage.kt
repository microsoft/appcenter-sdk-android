package com.microsoft.appcenter.sasquatch_kotlin.modules

import android.support.annotation.StringRes
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseModulePage : PreferenceFragmentCompat() {
    protected val uiScope = CoroutineScope(Dispatchers.Main)

    // region Helpers

    protected inline fun <reified T : Preference> getPreference(@StringRes id: Int): T =
            preferenceManager.findPreference(getString(id)) as T

    protected inline fun <reified T : Preference> updatePreference(@StringRes id: Int, crossinline block: suspend T.() -> Unit) =
            uiScope.launch {
                val preference = getPreference<T>(id)
                preference.block()
            }

    protected inline fun updateClickPreference(@StringRes id: Int, crossinline block: suspend () -> Unit) {
        val preference = getPreference<Preference>(id)
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            uiScope.launch { block() }
            true
        }
    }

    protected inline fun <reified T> onPreferenceChange(crossinline block: suspend (value: T) -> Unit) =
            Preference.OnPreferenceChangeListener { _, newValue ->
                uiScope.launch { block(newValue as T) }
                true
            }

    // endregion
}
