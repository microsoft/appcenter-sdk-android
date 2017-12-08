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
#include <limits.h>
#include "android/log.h"
#include "google-breakpad/src/client/linux/handler/exception_handler.h"
#include "google-breakpad/src/client/linux/handler/minidump_descriptor.h"

static google_breakpad::ExceptionHandler* exceptionHandler;

#ifdef __cplusplus
extern "C"
{
#endif

    /*
     * Triggered automatically after an attempt to write a minidump file to the breakpad folder.
     */
    bool DumpCallback(const google_breakpad::MinidumpDescriptor &descriptor,
                      void *context,
                      bool succeeded) {
        __android_log_print(ANDROID_LOG_INFO, "breakpad", "Dump path: %s\n", descriptor.path());
        return succeeded;
    }

    /**
     * Registers breakpad as the exception handler for NDK code.
     *
     * @param path returned from Crashes.getBreakpadDirectory()
     */
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
