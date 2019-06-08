package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.auth.SignInResult
import com.microsoft.appcenter.auth.Auth as JavaAuth

object Auth : AppCenterService {
    override val original = JavaAuth::class

    fun setConfigUrl(configUrl: String) = JavaAuth.setConfigUrl(configUrl)

    suspend fun isEnabled(): Boolean = JavaAuth.isEnabled().await()

    suspend fun setEnabled(enabled: Boolean) {
        JavaAuth.setEnabled(enabled).await()
    }

    suspend fun signIn(): SignInResult = JavaAuth.signIn().await()

    fun signOut() = JavaAuth.signOut()
}