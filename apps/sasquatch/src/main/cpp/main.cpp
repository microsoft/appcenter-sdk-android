/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <jni.h>
#include <string>
#include <stdio.h>
#include <string.h>
#include <limits.h>
#include "android/log.h"
#include "google-breakpad/src/client/linux/handler/exception_handler.h"
#include "google-breakpad/src/client/linux/handler/minidump_descriptor.h"

static google_breakpad::ExceptionHandler* exceptionHandler;

#ifdef __cplusplus
extern "C"
{
#endif

    bool DumpCallback(const google_breakpad::MinidumpDescriptor &descriptor,
                      void *context,
                      bool succeeded) {
        __android_log_print(ANDROID_LOG_INFO, "breakpad", "Dump path: %s\n", descriptor.path());
        return succeeded;
    }

    void Java_com_microsoft_appcenter_sasquatch_activities_MainActivity_setupNativeCrashesListener(JNIEnv *env, jobject, jstring path) {
        const char* dump_path = (char *)env->GetStringUTFChars(path, NULL);

        google_breakpad::MinidumpDescriptor descriptor(dump_path);
        exceptionHandler = new google_breakpad::ExceptionHandler(descriptor, NULL, DumpCallback, NULL, true, -1);

        env->ReleaseStringUTFChars(path, dump_path);
    }

    jint JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {

        __android_log_print(ANDROID_LOG_INFO, "breakpad", "JNI onLoad...");

        return JNI_VERSION_1_4;
    }


    void Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeDivideByZeroCrash(JNIEnv* env, jobject obj) {
        volatile int *a = reinterpret_cast<volatile int *>(NULL);
        *a = 1;
    }

    void Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeStackOverflowCrash(JNIEnv* env, jobject obj) {
        Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeStackOverflowCrash(env,
                                                                                                 obj);

    }

    void Java_com_microsoft_appcenter_sasquatch_activities_CrashActivity_nativeOutOfMemoryCrash(JNIEnv* env, jobject obj) {
        uint size = UINT_MAX;
        int* array = new int[size];
    }

#ifdef __cplusplus
}
#endif
