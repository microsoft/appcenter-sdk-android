/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.crashes.Crashes as JavaCrashes

object Crashes : AppCenterService {
    override val original = JavaCrashes::class

}