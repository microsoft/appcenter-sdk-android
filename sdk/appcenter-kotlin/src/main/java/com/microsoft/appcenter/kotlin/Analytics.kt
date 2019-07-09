/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.analytics.EventProperties
import com.microsoft.appcenter.analytics.Analytics as JavaAnalytics

object Analytics : AppCenterService {
    override val original = JavaAnalytics::class

    suspend fun isEnabled(): Boolean = JavaAnalytics.isEnabled().await()

    suspend fun setEnabled(enabled: Boolean) {
        JavaAnalytics.setEnabled(enabled).await()
    }

    fun setTransmissionInterval(seconds: Int): Boolean = JavaAnalytics.setTransmissionInterval(seconds)

    fun pause() = JavaAnalytics.pause()

    fun resume() = JavaAnalytics.resume()

    fun trackEvent(name: String) = JavaAnalytics.trackEvent(name)

    fun trackEvent(name: String, properties: Map<String, String>) = JavaAnalytics.trackEvent(name, properties)

    fun trackEvent(name: String, properties: Map<String, String>, flags: Int) = JavaAnalytics.trackEvent(name, properties, flags)

    fun trackEvent(name: String, properties: EventProperties) = JavaAnalytics.trackEvent(name, properties)

    fun trackEvent(name: String, properties: EventProperties, flags: Int) = JavaAnalytics.trackEvent(name, properties, flags)
}
