[![Build Status](https://www.bitrise.io/app/78891228f9c6e6dc.svg?token=KQ6kVAci490XBjulCcQuGQ&branch=develop)](https://www.bitrise.io/app/78891228f9c6e6dc)
[![codecov](https://codecov.io/gh/Microsoft/MobileCenter-SDK-Android/branch/master/graph/badge.svg?token=YwMZRPnYK3)](https://codecov.io/gh/Microsoft/MobileCenter-SDK-Android)

# Mobile Center SDK for Android

## Introduction

The Microsoft Mobile Center Android SDK lets you add Mobile Center services to your Android application.

The SDK is currently in private beta release and we support the following services:

1. **Analytics**: Mobile Center Analytics helps you understand user behavior and customer engagement to improve your Android app. The SDK automatically captures session count and device properties like model, OS Version etc. You can define your own custom events to measure things that matter to your business. All the information captured is available in the Mobile Center portal for you to analyze the data.

2. **Crashes**: The Mobile Center SDK will automatically generate a crash log every time your app crashes. The log is first written to the device's storage and when the user starts the app again, the crash report will be forwarded to Mobile Center. Collecting crashes works for both beta and live apps, i.e. those submitted to Google Play or other app stores. Crash logs contain viable information for you to help resolve the issue. The SDK gives you a lot of flexibility how to handle a crash log. As a developer you can collect and add additional information to the report if you like.

This document contains the following sections:

1. [Prerequisites](#1-prerequisites)
2. [Add Mobile Center SDK modules](#2-add-mobile-center-sdk-modules)
3. [Start the SDK](#3-start-the-sdk)
4. [Analytics APIs](#4-analytics-apis)
5. [Crashes APIs](#5-crashes-apis)
6. [Advanced APIs](#6-advanced-apis)
7. [Troubleshooting](#7-troubleshooting)
8. [List of available libraries](#8-list-of-available-libraries)


Let's get started with setting up Mobile Center Android SDK in your app to use these services:

## 1. Prerequisites

Before you begin, please make sure that the following prerequisites are met:

* An Android project that is set up in Android Studio.
* A device running Android Version 4.0.3 or API level 15 or higher.

## 2. Add Mobile Center SDK modules

The Mobile Center SDK is designed with a modular approach – a developer only needs to integrate the modules of the services that they're interested in.

Below are the steps on how to integrate our compiled libraries in your application using Android Studio and Gradle.

1. Open your app level build.gradle file (app/build.gradle) and add the following lines after `apply plugin`. During the private beta, you need to include these credentials in order to get the libraries.

    ```groovy
    repositories {
        maven {
            url  'http://microsoftsonoma.bintray.com/mobilecenter'
            credentials {
                username 'sonoma-beta'
                password 'cbaf881993ad1ab4128f71da716d060e8590906a'
            }
        }
    }
    ```

2. In the same file, include the dependencies that you want in your project. Each SDK module needs to be added as a separate dependency in this section. If you would want to use both Analytics and Crashes, add the following lines:

    ```groovy
    dependencies {
        def mobileCenterSdkVersion = '0.2.0'
        compile "com.microsoft.azure.mobile:mobile-center-analytics:${mobileCenterSdkVersion}"
        compile "com.microsoft.azure.mobile:mobile-center-crashes:${mobileCenterSdkVersion}"
    }
    ```

3. Save your build.gradle file and make sure to trigger a Gradle sync in Android Studio.

Now that you've integrated the SDK in your application, it's time to start the SDK and make use of Mobile Center services.

## 3. Start the SDK

To start the Mobile Center SDK in your app, follow these steps:

1. **Start the SDK:**  Mobile Center provides developers with two modules to get started – Analytics and Crashes. In order to use these modules, you need to opt in for the module(s) that you'd like, meaning by default no modules are started and you will have to explicitly call each of them when starting the SDK. Insert the following line inside your app's main activity class' `onCreate` callback.

    ```Java
    MobileCenter.start(getApplication(), "{Your App Secret}", Analytics.class, Crashes.class);
    ```
    You can also copy paste the `start` method call from the Overview page on Mobile Center portal once your app is selected. It already includes the App Secret so that all the data collected by the SDK corresponds to your application. Make sure to replace {Your App Secret} text with the actual value for your application.
    
    The example above shows how to use the `start()` method and include both the Analytics and Crashes module. If you wish not to use Analytics, remove the parameter from the method call above. Note that, unless you explicitly specify each module as parameters in the start method, you can't use that Mobile Center service. Also, the `start()` API can be used only once in the lifecycle of your app – all other calls will log a warning to the console and only the modules included in the first call will be available.

    Android Studio will automatically suggest the required import statements once you insert the `start()` method-call, but if you see an error that the class names are not recognized, add the following lines to the import statements in your activity class:
    
    ```Java
    import com.microsoft.azure.mobile.MobileCenter;
    import com.microsoft.azure.mobile.analytics.Analytics;
    import com.microsoft.azure.mobile.crashes.Crashes;
    ```

## 4. Analytics APIs

* **Track Session, Device Properties:**  Once the Analytics module is included in your app and the SDK is started, it will automatically track sessions, device properties like OS Version, model, manufacturer etc. and you don’t need to add any additional code.
    Look at the section above on how to [Start the SDK](#3-start-the-sdk) if you haven't started it yet.

* **Custom Events:** You can track your own custom events with specific properties to know what's happening in your app, understand user actions, and see the aggregates in the Mobile Center portal. Once you have started the SDK, use the `trackEvent()` method to track your events with properties.

    ```Java
    Map<String,String>properties=new HashMap<String,String>();
    properties.put("Category", "Music");
    properties.put("FileName", "favorite.avi");

    Analytics.trackEvent("Video clicked", properties);
    ```

    Of course, properties for events are entirely optional – if you just want to track an event use this sample instead:

    ```Java
    Analytics.trackEvent("Video clicked");
    ```

* **Enable or disable Analytics:**  You can change the enabled state of the Analytics module at runtime by calling the `Analytics.setEnabled()` method. If you disable it, the SDK will not collect any more analytics information for the app. To re-enable it, pass `true` as a parameter in the same method.

    ```Java
    Analytics.setEnabled(false);
    ```

    You can also check, if the module is enabled or not using the `isEnabled()` method:

    ```Java
    Analytics.isEnabled();
    ```

## 5. Crashes APIs

Once you set up and start the Mobile Center SDK to use the Crashes module in your application, the SDK will automatically start logging any crashes in the device's local storage. When the user opens the application again, all pending crash logs will automatically be forwarded to Mobile Center and you can analyze the crash along with the stack trace on the Mobile Center portal. Refer to the section to [Start the SDK](#3-start-the-sdk) if you haven't done so already.

* **Generate a test crash:** The SDK provides you with a static API to generate a test crash for easy testing of the SDK:

    ```Java
    Crashes.generateTestCrash();
    ```

    Note that this API can only be used in test/beta apps and won't work in production apps.

* **Did the app crash in last session:** At any time after starting the SDK, you can check if the app crashed in the previous session:

    ```Java
    Crashes.hasCrashedInLastSession();
    ```

* **Details about the last crash:** If your app crashed previously, you can get details about the last crash:

    ```Java
    ErrorReport crashReport = Crashes.getLastSessionCrashReport();
    ```

* **Enable or disable the Crashes module:**  You can disable and opt out of using the Crashes module by calling the `setEnabled()` API and the SDK will collect no crashes for your app. Use the same API to re-enable it by passing `true` as a parameter.

    ```Java
    Crashes.setEnabled(false);
    ```

    You can also check if the module is enabled or not using the `isEnabled()` method:

    ```Java
    Crashes.isEnabled();
    ```

* **Advanced Scenarios:**  The Crashes module provides callbacks for developers to perform additional actions before and when sending crash reports to Mobile Center. This gives you added flexibility on the crash reports that will be sent.
To handle the callbacks, you must either implement all methods in the `CrashesListener` interface, or override the `AbstractCrashesListener` class and pick only the ones you're interested in.
You create your own Crashes listener and assign it like this:

    ```Java
    CrashesListener customListener = new CrashesListener() {
        // implement callbacks as seen below
    };

    Crashes.setListener(customListener);
    ```

    The following callbacks are provided:

    * **Should the crash be processed:** Implement this callback if you'd like to decide if a particular crash needs to be processed or not. For example - there could be some system level crashes that you'd want to ignore and don't want to send to Mobile Center.

        ```Java
        boolean CrashesListener.shouldProcess(ErrorReport report) {
            return true; // return true if the crash report should be processed, otherwise false.
        }
        ```

    * **User Confirmation:** If user privacy is important to you as a developer, you might want to get user confirmation before sending a crash report to Mobile Center. The SDK exposes a callback where you can tell it to await user confirmation before sending any crash reports.
    Your app is then responsible for obtaining confirmation, e.g. through a dialog prompt with one of these options - "Always Send", "Send", and "Don't send". Based on the user input, you will tell the SDK and the crash will then respectively be forwarded to Mobile Center or not.

        ```Java
        boolean CrashesListener.shouldAwaitUserConfirmation() {
            return true; // Return true if the SDK should await user confirmation, otherwise false.
        }
        ```

        If you return `true`, your app should obtain user permission and message the SDK with the result using the following API:

        ```Java
        Crashes.notifyUserConfirmation(int userConfirmation);
        ```
        Pass one option of `SEND`, `DONT_SEND` or `ALWAYS_SEND`.

    * **Binary attachment:**  If you'd like to attach text/binary data to a crash report, implement this callback. Before sending the crash, our SDK will add the attachment to the crash report and you can view it on the Mobile Center portal.   

        ```Java
        ErrorAttachment CrashesListener.getErrorAttachment(ErrorReport report) {
            // return your own created ErrorAttachment object
        }
        ```

    * **Before sending a crash report:** This callback will be invoked just before the crash is sent to Mobile Center:

        ```Java
        void CrashesListener.onBeforeSending(ErrorReport report) {
            …
        }
        ```

    * **When sending a crash report succeeded:** This callback will be invoked after sending a crash report succeeded:

        ```Java
        void CrashesListener.onSendingSucceeded(ErrorReport report) {
            …
        }
        ```

    * **When sending a crash report failed:** This callback will be invoked after sending a crash report failed:

        ```Java
        void CrashesListener.onSendingFailed(ErrorReport report, Exception e) {
            …
        }
        ```

## 6. Advanced APIs

* **Debugging**: You can control the amount of log messages that show up from the Mobile Center SDK in LogCat. Use the `MobileCenter.setLogLevel()` API to enable additional logging while debugging. The log levels correspond to the ones defined in `android.util.Log`. By default, it is set it to `ASSERT` for non-debuggable applications and `WARN` for debuggable applications.

    ```Java
        MobileCenter.setLogLevel(Log.VERBOSE);
    ```

* **Get Install Identifier**: The Mobile Center SDK creates a UUID for each device once the app is installed. This identifier remains the same for a device when the app is updated and a new one is generated only when the app is re-installed. The following API is useful for debugging purposes.

    ```Java
        UUID installId = MobileCenter.getInstallId();
    ```

* **Enable/Disable Mobile Center SDK:** If you want the Mobile Center SDK to be disabled completely, use the `setEnabled()` API. When disabled, the SDK will not forward any information to Mobile Center.

    ```Java
        MobileCenter.setEnabled(false);
    ```

## 7. Troubleshooting

* **How long to wait for Analytics data to appear on the portal?**

* **How long do I have to wait for crashes to appear on the portal?**
    Make sure your device has a working internet connection and restart the crashed app. The crash reports should appear on the portal within a few minutes.

* **Do I need to include all the modules? Is there anything included by default?**
    You only include the modules for the services you want to use. They all have a dependency on the Mobile Center module, so this will be included once you pull down the dependencies.

* **Debugging steps, when you can't see crash reports on the portal:**
    1. Make sure the SDK `start()` API is used correctly and the Crashes module is initialized. Also, you need to restart the app after a crash – our SDK will forward the crash log only after it's restarted.
    2. Make sure your device is connected to the internet.
    3. Check if the App Secret used to start the SDK matches the App Secret in the Mobile Center portal.
    4. Disable any other SDK that provides Crash Reporting functionality, as those might interfere with the Mobile Center SDK.
    5. Gather additional information through [debug logs](#6-advanced-apis) and have them ready when you contact our SDK support.

* **What data does SDK automatically collect for Analytics?**

* **What Android permissions are required for the SDK?**
    Depending on the services you use, the following permissions are required:
    - Analytics, Crashes: `INTERNET`, `ACCESS_NETWORK_STATE`

    Required permissions will automatically be merged into your app's manifest by the SDK. Where possible, the SDK will use [Run Time permissions](https://developer.android.com/training/permissions/requesting.html).

* **Any privacy information tracked by SDK?**

## 8. List of available libraries

 Gradle Dependency                                            | Service          
 ------------------------------------------------------------ | ---------------
 com.microsoft.azure.mobile:mobile-center-analytics:0.2.0     | Analytics    
 com.microsoft.azure.mobile:mobile-center-crashes:0.2.0       | Crashes
