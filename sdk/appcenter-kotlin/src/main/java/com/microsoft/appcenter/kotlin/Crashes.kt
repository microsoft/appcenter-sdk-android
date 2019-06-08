/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.crashes.CrashesListener
import com.microsoft.appcenter.crashes.model.ErrorReport
import com.microsoft.appcenter.crashes.Crashes as JavaCrashes

object Crashes : AppCenterService {
    override val original = JavaCrashes::class

    suspend fun isEnabled(): Boolean = JavaCrashes.isEnabled().await()

    suspend fun setEnabled(enabled: Boolean) {
        JavaCrashes.setEnabled(enabled).await()
    }

    fun generateTestCrash() = JavaCrashes.generateTestCrash()

    fun setListener(listener: CrashesListener) = JavaCrashes.setListener(listener)

    suspend fun getMinidumpDirectory(): String = JavaCrashes.getMinidumpDirectory().await()

    fun notifyUserConfirmation(userConfirmation: Int) = JavaCrashes.notifyUserConfirmation(userConfirmation)

    suspend fun hasCrashedInLastSession(): Boolean = JavaCrashes.hasCrashedInLastSession().await()

    suspend fun getLastSessionCrashReport(): ErrorReport = JavaCrashes.getLastSessionCrashReport().await()
}