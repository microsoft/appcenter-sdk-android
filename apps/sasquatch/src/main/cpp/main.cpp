/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#include <jni.h>
#include <string>
#include <stdio.h>
#include <string.h>
#include "android/log.h"
#include "google-breakpad/src/client/linux/handler/exception_handler.h"
#include "google-breakpad/src/client/linux/handler/minidump_descriptor.h"

#ifdef __cplusplus
extern "C"
{
#endif

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

/*
 * Triggered automatically after an attempt to write a minidump file to the breakpad folder.
 */
bool dumpCallback(const google_breakpad::MinidumpDescriptor &descriptor,
                  void *context,
                  bool succeeded) {

    /* Allow system to log the native stack trace. */
    __android_log_print(ANDROID_LOG_INFO, "AppCenterSasquatch",
                        "Wrote breakpad minidump at %s succeeded=%d\n", descriptor.path(),
                        succeeded);
    return false;
}
#pragma clang diagnostic pop

/**
 * Registers breakpad as the exception handler for NDK code.
 * @param env JNI environment.
 * @param path minidump directory path returned from Crashes.getMinidumpDirectory()
 */
void Java_com_microsoft_appcenter_sasquatch_activities_MainActivity_setupNativeCrashesListener(
        JNIEnv *env, jobject, jstring path) {
    const char *dumpPath = (char *) env->GetStringUTFChars(path, NULL);
    google_breakpad::MinidumpDescriptor descriptor(dumpPath);
    new google_breakpad::ExceptionHandler(descriptor, NULL, dumpCallback, NULL, true, -1);
    env->ReleaseStringUTFChars(path, dumpPath);
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
jint JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    __android_log_print(ANDROID_LOG_INFO, "AppCenterSasquatch", "JNI OnLoad");
    return JNI_VERSION_1_4;
}
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma ide diagnostic ignored "OCDFAInspection"
void
Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeDereferenceNullPointer(
        JNIEnv *env,
        jobject obj) {
    volatile int *a = reinterpret_cast<volatile int *>(NULL);
    *a = 1;
}
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma ide diagnostic ignored "InfiniteRecursion"
void Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeStackOverflowCrash(
        JNIEnv *env, jobject obj) {
    Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeStackOverflowCrash(env,
                                                                                             obj);

    /* Defeat release build optimization by adding another statement after recursion. */
    __android_log_print(ANDROID_LOG_INFO, "AppCenterSasquatch",
                        "You will never see this log after an infinite loop.");
}
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void
Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeAbortCall(JNIEnv *env,
                                                                                jobject obj) {
    abort();
}
#pragma clang diagnostic pop

#ifdef __cplusplus
}
#endif
