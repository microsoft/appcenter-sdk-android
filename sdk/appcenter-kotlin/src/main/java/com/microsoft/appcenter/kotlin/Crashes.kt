/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.crashes.CrashesListener
import com.microsoft.appcenter.crashes.model.ErrorReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.microsoft.appcenter.crashes.Crashes as JavaCrashes

object Crashes : AppCenterService {
    override val original = JavaCrashes::class

    suspend fun isEnabled(): Boolean = suspendCoroutine { c ->
        JavaCrashes.isEnabled().thenAccept { c.resume(it) }
    }

    suspend fun setEnabled(enabled: Boolean): Unit = suspendCoroutine { c ->
        JavaCrashes.setEnabled(enabled).thenAccept { c.resume(Unit) }
    }

    fun generateTestCrash() = JavaCrashes.generateTestCrash()

    fun setListener(listener: CrashesListener) = JavaCrashes.setListener(listener)

    suspend fun getMinidumpDirectory(): String = suspendCoroutine { c ->
        JavaCrashes.getMinidumpDirectory().thenAccept { c.resume(it) }
    }

    fun notifyUserConfirmation(userConfirmation: Int) = JavaCrashes.notifyUserConfirmation(userConfirmation)

    suspend fun hasCrashedInLastSession(): Boolean = suspendCoroutine { c ->
        JavaCrashes.hasCrashedInLastSession().thenAccept { c.resume(it) }
    }

    suspend fun getLastSessionCrashReport(): ErrorReport = suspendCoroutine { c ->
        JavaCrashes.getLastSessionCrashReport().thenAccept { c.resume(it) }
    }
}