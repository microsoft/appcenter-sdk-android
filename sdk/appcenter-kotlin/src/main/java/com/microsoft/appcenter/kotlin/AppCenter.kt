/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import android.app.Application
import android.content.Context
import com.microsoft.appcenter.CustomProperties
import java.util.*
import com.microsoft.appcenter.AppCenter as JavaAppCenter

object AppCenter {

    val sdkVersion: String
        get() = JavaAppCenter.getSdkVersion()

    var logLevel: Int
        get() = JavaAppCenter.getLogLevel()
        set(value) = JavaAppCenter.setLogLevel(value)

    fun setLogUrl(logUrl: String) = JavaAppCenter.setLogUrl(logUrl)

    fun setCustomProperties(customProperties: CustomProperties) = JavaAppCenter.setCustomProperties(customProperties)

    val isConfigured: Boolean
        get() = JavaAppCenter.isConfigured()

    fun configure(application: Application, appSecret: String) = JavaAppCenter.configure(application, appSecret)

    fun configure(application: Application) = JavaAppCenter.configure(application)

    fun start(vararg services: AppCenterService) {
        val javaServices = services.map { it.original.java }.toTypedArray()
        JavaAppCenter.start(*javaServices)
    }

    fun start(application: Application, appSecret: String, vararg services: AppCenterService) {
        val javaServices = services.map { it.original.java }.toTypedArray()
        JavaAppCenter.start(application, appSecret, *javaServices)
    }

    fun start(application: Application, vararg services: AppCenterService) {
        val javaServices = services.map { it.original.java }.toTypedArray()
        JavaAppCenter.start(application, *javaServices)
    }

    fun startFromLibrary(context: Context, vararg services: AppCenterService) {
        val javaServices = services.map { it.original.java }.toTypedArray()
        JavaAppCenter.startFromLibrary(context, *javaServices)
    }

    suspend fun isEnabled(): Boolean = JavaAppCenter.isEnabled().await()

    suspend fun setEnabled(enabled: Boolean) {
        JavaAppCenter.setEnabled(enabled).await()
    }

    suspend fun getInstallId(): UUID? = JavaAppCenter.getInstallId().await()

    suspend fun setMaxStorageSize(storageSizeInBytes: Long): Boolean = JavaAppCenter.setMaxStorageSize(storageSizeInBytes).await()

    fun setUserId(userId: String) = JavaAppCenter.setUserId(userId)
}