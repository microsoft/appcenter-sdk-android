/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.analytics.EventProperties
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.microsoft.appcenter.analytics.Analytics as JavaAnalytics

object Analytics : AppCenterService {
    override val original = JavaAnalytics::class

    suspend fun isEnabled(): Boolean = suspendCoroutine { c ->
        JavaAnalytics.isEnabled().thenAccept { c.resume(it) }
    }

    suspend fun setEnabled(enabled: Boolean): Unit = suspendCoroutine { c ->
        JavaAnalytics.setEnabled(enabled).thenAccept { c.resume(Unit) }
    }

    fun setTransmissionInterval(seconds: Int): Boolean = JavaAnalytics.setTransmissionInterval(seconds)

    fun pause() = JavaAnalytics.pause()

    fun resume() = JavaAnalytics.pause()

    fun trackEvent(name: String) = JavaAnalytics.trackEvent(name)

    fun trackEvent(name: String, properties: Map<String, String>) = JavaAnalytics.trackEvent(name, properties)

    fun trackEvent(name: String, properties: Map<String, String>, flags: Int) = JavaAnalytics.trackEvent(name, properties, flags)

    fun trackEvent(name: String, properties: EventProperties) = JavaAnalytics.trackEvent(name, properties)

    fun trackEvent(name: String, properties: EventProperties, flags: Int) = JavaAnalytics.trackEvent(name, properties, flags)
}