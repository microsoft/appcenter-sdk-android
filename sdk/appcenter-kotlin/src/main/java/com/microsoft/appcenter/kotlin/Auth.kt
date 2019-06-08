package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.auth.SignInResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.microsoft.appcenter.auth.Auth as JavaAuth

object Auth : AppCenterService {
    override val original = JavaAuth::class

    fun setConfigUrl(configUrl: String) = JavaAuth.setConfigUrl(configUrl)

    suspend fun isEnabled(): Boolean = suspendCoroutine { c ->
        JavaAuth.isEnabled().thenAccept { c.resume(it) }
    }

    suspend fun setEnabled(enabled: Boolean): Unit = suspendCoroutine { c ->
        JavaAuth.setEnabled(enabled).thenAccept { c.resume(Unit) }
    }

    suspend fun signIn(): SignInResult = suspendCoroutine { c ->
        JavaAuth.signIn().thenAccept { c.resume(it) }
    }

    fun signOut() = JavaAuth.signOut()
}