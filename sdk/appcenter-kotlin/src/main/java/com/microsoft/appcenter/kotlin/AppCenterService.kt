/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import kotlin.reflect.KClass
import com.microsoft.appcenter.AppCenterService as JavaAppCenterService

interface AppCenterService {
    val original: KClass<out JavaAppCenterService>
}