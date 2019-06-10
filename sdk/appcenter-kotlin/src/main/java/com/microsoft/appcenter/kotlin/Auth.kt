package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.auth.UserInformation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.microsoft.appcenter.auth.Auth as JavaAuth

object Auth : AppCenterService {
    override val original = JavaAuth::class

    fun setConfigUrl(configUrl: String) = JavaAuth.setConfigUrl(configUrl)

    suspend fun isEnabled(): Boolean = JavaAuth.isEnabled().await()

    suspend fun setEnabled(enabled: Boolean) {
        JavaAuth.setEnabled(enabled).await()
    }

    suspend fun signIn(): UserInformation = JavaAuth.signIn().await {
        if (it.exception != null) {
            resumeWithException(it.exception)
        } else {
            resume(it.userInformation)
        }
    }

    fun signOut() = JavaAuth.signOut()
}