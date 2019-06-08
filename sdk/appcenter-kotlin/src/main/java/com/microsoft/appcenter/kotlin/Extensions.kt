package com.microsoft.appcenter.kotlin

import com.microsoft.appcenter.utils.async.AppCenterFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


public suspend fun <T> AppCenterFuture<T>.await(): T =
        suspendCoroutine { continuation ->

            /*
             * thenAccept { continuation.resume(it) }
             *
             * Do not use "thenAccept" here to avoid deadlocks since it uses "runOnUiThread".
             * It works fine with the simple cases, but doesn't when user uses "runBlocking" from
             * the main thread. Continuation interceptors are taking care about callback thread instead.
             */
            if (isDone) {
                continuation.resume(get())
            } else {

                // IO threads can be blocked for long operations.
                GlobalScope.launch(Dispatchers.IO) {
                    continuation.resume(get())
                }
            }
        }

