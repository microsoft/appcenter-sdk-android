[![Build Status](https://www.bitrise.io/app/78891228f9c6e6dc.svg?token=KQ6kVAci490XBjulCcQuGQ&branch=develop)](https://www.bitrise.io/app/78891228f9c6e6dc)
[![codecov](https://codecov.io/gh/Microsoft/mobile-center-sdk-android/branch/develop/graph/badge.svg?token=YwMZRPnYK3)](https://codecov.io/gh/Microsoft/mobile-center-sdk-android)
[![GitHub Release](https://img.shields.io/github/release/Microsoft/mobile-center-sdk-android.svg)](https://github.com/Microsoft/mobile-center-sdk-android/releases/latest)
[![Bintray](https://api.bintray.com/packages/mobile-center/mobile-center/mobile-center/images/download.svg)](https://bintray.com/mobile-center/mobile-center)
[![license](https://img.shields.io/badge/license-MIT%20License-00AAAA.svg)](https://github.com/Microsoft/mobile-center-sdk-android/blob/develop/license.txt)

# Mobile Center SDK for Android

## Introduction

The Microsoft Mobile Center Android SDK lets you add Mobile Center services to your Android application.

The SDK is currently in private beta release and we support the following services:

1. **Analytics**: Mobile Center Analytics helps you understand user behavior and customer engagement to improve your Android app. The SDK automatically captures session count and device properties like model, OS Version etc. You can define your own custom events to measure things that matter to your business. All the information captured is available in the Mobile Center portal for you to analyze the data.

2. **Crashes**: The Mobile Center SDK will automatically generate a crash log every time your app crashes. The log is first written to the device's storage and when the user starts the app again, the crash report will be forwarded to Mobile Center. Collecting crashes works for both beta and live apps, i.e. those submitted to Google Play or other app stores. Crash logs contain viable information for you to help resolve the issue. The SDK gives you a lot of flexibility how to handle a crash log. As a developer you can collect and add additional information to the report if you like.

3. **Distribute**: Our SDK will let your users install a new version of the app when you distribute it via Mobile Center. With a new version of the app available, the SDK will present an update dialog to the users to either download or ignore the latest version. Once they click "Download", SDK will start the installation process of your application. Note that this feature will `NOT` work if your app is deployed to the app store, if you are developing locally or if the app is a debug build.

This document contains the following sections:

1. [Prerequisites](#1-prerequisites)
2. [Add Mobile Center SDK modules](#2-add-mobile-center-sdk-modules)
3. [Start the SDK](#3-start-the-sdk)
4. [Analytics APIs](#4-analytics-apis)
5. [Crashes APIs](#5-crashes-apis)
6. [Distribute APIs](#6-distribute-apis)
7. [Advanced APIs](#7-advanced-apis)
8. [Troubleshooting](#8-troubleshooting)
9. [Contributing](#9-contributing)
10. [Contact](#10-contact)

Let's get started with setting up Mobile Center Android SDK in your app to use these services:

## 1. Prerequisites

Before you begin, please make sure that the following prerequisites are met:

* An Android project that is set up in Android Studio.
* A device running Android Version 4.0.3 or API level 15 or higher.

## 2. Add Mobile Center SDK modules

The Mobile Center SDK is designed with a modular approach – a developer only needs to integrate the modules of the services that they're interested in.

Below are the steps on how to integrate our compiled libraries in your application using Android Studio and Gradle.

1. Open your app level build.gradle file (app/build.gradle) and include the dependencies that you want in your project. Each SDK module needs to be added as a separate dependency in this section. If you want to include all the services - Analytics, Crashes and Distribute, add the following lines:

    ```groovy
    dependencies {
        def mobileCenterSdkVersion = '0.5.0'
        compile "com.microsoft.azure.mobile:mobile-center-analytics:${mobileCenterSdkVersion}"
        compile "com.microsoft.azure.mobile:mobile-center-crashes:${mobileCenterSdkVersion}"
        compile "com.microsoft.azure.mobile:mobile-center-distribute:${mobileCenterSdkVersion}"
    }
    ```
You can remove the dependency line for the service that you don't want to include in your app.

2. Save your build.gradle file and make sure to trigger a Gradle sync in Android Studio.

Now that you've integrated the SDK in your application, it's time to start the SDK and make use of Mobile Center services.

## 3. Start the SDK

To start the Mobile Center SDK in your app, follow these steps:

1. **Start the SDK:**  Mobile Center provides developers with these services to get started – Analytics, Crashes and Distribute. In order to use these services, you need to opt in for the service(s) that you'd like, meaning by default no services are started and you will have to explicitly call each of them when starting the SDK. Insert the following line inside your app's main activity class' `onCreate` callback.

    ```Java
    MobileCenter.start(getApplication(), "{Your App Secret}", Analytics.class, Crashes.class, Distribute.class);
    ```
    You can also copy paste the `start` method call from the Overview page on Mobile Center portal once your app is selected. It already includes the App Secret so that all the data collected by the SDK corresponds to your application. Make sure to replace {Your App Secret} text with the actual value for your application.
    
    The example above shows how to use the `start()` method and include Analytics, Crashes and Distribute services. If you wish not to onboard to any of these services, say you dont want to use features provided by Distribute service, remove the parameter from the method call above. Note that, unless you explicitly specify each service as parameters in the start method, you can't use that Mobile Center service. Also, the `start()` API can be used only once in the lifecycle of your app – all other calls will log a warning to the console and only the services included in the first call will be available.

    For example - if you just want to onboard to Analytics service, you should modify the start() API call like below:
    ```Java
    MobileCenter.start(getApplication(), "{Your App Secret}", Analytics.class);
    ```

    Android Studio will automatically suggest the required import statements once you insert the `start()` method-call, but if you see an error that the class names are not recognized, add the following lines to the import statements in your activity class:
    
    ```Java
    import com.microsoft.azure.mobile.MobileCenter;
    import com.microsoft.azure.mobile.analytics.Analytics;
    import com.microsoft.azure.mobile.crashes.Crashes;
    import com.microsoft.azure.mobile.distribute.Distribute;
    ```

## 4. Analytics APIs

* **Track Session, Device Properties:**  Once the Analytics service is included in your app and the SDK is started, it will automatically track sessions, device properties like OS Version, model, manufacturer etc. and you don’t need to add any additional code.
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

* **Enable or disable Analytics:**  You can change the enabled state of the Analytics service at runtime by calling the `Analytics.setEnabled()` method. If you disable it, the SDK will not collect any more analytics information for the app. To re-enable it, pass `true` as a parameter in the same method.

    ```Java
    Analytics.setEnabled(false);
    ```

    You can also check if the service is enabled or not using the `isEnabled()` method:

    ```Java
    Analytics.isEnabled();
    ```

## 5. Crashes APIs

Once you set up and start the Mobile Center SDK to use the Crashes service in your application, the SDK will automatically start logging any crashes in the device's local storage. When the user opens the application again, all pending crash logs will automatically be forwarded to Mobile Center and you can analyze the crash along with the stack trace on the Mobile Center portal. Refer to the section to [Start the SDK](#3-start-the-sdk) if you haven't done so already.

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
    Crashes.getLastSessionCrashReport(new ResultCallback<ErrorReport>() {

        @Override
        public void onResult(ErrorReport data) {
            if (data != null) {
                Log.i("MyApp", "Last session crash details=", data.getThrowable());
            }
        }
    });
    ```

* **Enable or disable Crashes:**  You can disable and opt out of using Crashes by calling the `setEnabled()` API and the SDK will collect no crashes for your app. Use the same API to re-enable it by passing `true` as a parameter.

    ```Java
    Crashes.setEnabled(false);
    ```

    You can also check if the service is enabled or not using the `isEnabled()` method:

    ```Java
    Crashes.isEnabled();
    ```

* **Advanced Scenarios:**  The Crashes service provides callbacks for developers to perform additional actions before and when sending crash reports to Mobile Center. This gives you added flexibility on the crash reports that will be sent.
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

    * **User Confirmation:** By default the SDK automatically sends crash reports to Mobile Center. However, the SDK exposes a callback where you can tell it to await user confirmation before sending any crash reports.
    Your app is then responsible for obtaining confirmation, e.g. through a dialog prompt with one of these options - "Always Send", "Send", and "Don't Send". Based on the user input, you will tell the SDK and the crash will then respectively be forwarded to Mobile Center or not.

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

## 6. Distribute APIs

You can easily let your users get the latest version of your app by integrating `Distribute` service of Mobile Center SDK. All you need to do is pass the service name as a parameter in the `start()` API call. Once the activity is created, the SDK checks for new updates in the background. If it finds a new update, users will see a dialog with three options - `Download`,`Postpone` and `Ignore`. If the user presses `Download`, it will trigger the new version to be installed. Postpone will delay the download until the app is opened again. Ignore will not prompt the user again for that particular app version.

You can easily provide your own resource strings if you'd like to localize the text displayed in the update dialog. Look at the string files [here](https://github.com/Microsoft/mobile-center-sdk-android/blob/distribute/sdk/mobile-center-distribute/src/main/res/values/strings.xml). Use the same string name and specify the localized value to be reflected in the dialog in your own app resource files.  

* **Enable or disable Distribute:**  You can change the enabled state by calling the `Distribute.setEnabled()` method. If you disable it, the SDK will not prompt your users when a new version is available for install. To re-enable it, pass `true` as a parameter in the same method.

    ```Java
    Distribute.setEnabled(false);
    ```

    You can also check if the service is enabled or not using the `isEnabled()` method. Note that it will only disable SDK features for Distribute service which is in-app updates for your application and has nothing to do with disabling `Distribute` service from Mobile Center.

    ```Java
    Distribute.isEnabled();
    ```
        

## 7. Advanced APIs

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

## 8. Troubleshooting

* **How long to wait for Analytics data to appear on the portal?**

* **How long do I have to wait for crashes to appear on the portal?**
    Make sure your device has a working internet connection and restart the crashed app. The crash reports should appear on the portal within a few minutes.

* **Do I need to include all the modules? Is there anything included by default?**
    You only include the modules for the services you want to use. They all have a dependency on the Mobile Center module, so this will be included once you pull down the dependencies.

* **Debugging steps, when you can't see crash reports on the portal:**
    1. Make sure the SDK `start()` API is used correctly and the Crashes service is configured. Also, you need to restart the app after a crash – our SDK will forward the crash log only after it's restarted.
    2. Make sure your device is connected to the internet.
    3. Check if the App Secret used to start the SDK matches the App Secret in the Mobile Center portal.
    4. Disable any other SDK that provides Crash Reporting functionality, as those might interfere with the Mobile Center SDK.
    5. Gather additional information through [debug logs](#6-advanced-apis) and have them ready when you contact our SDK support.

* **What data does SDK automatically collect for Analytics?**

* **What Android permissions are required for the SDK?**
    Depending on the services you use, the following permissions are required:
    - All services: `INTERNET`, `ACCESS_NETWORK_STATE`
    - Distribute: `REQUEST_INSTALL_PACKAGES`, `DOWNLOAD_WITHOUT_NOTIFICATION`

    Required permissions are automatically merged into your app's manifest by the SDK.
    
    None of these permissions require user approval at runtime, those are all install time permissions.

## 9. Contributing

We're looking forward to your contributions via pull requests.

### 9.1 Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact opencode@microsoft.com with any additional questions or comments.

### 9.2 Contributor License

You must sign a [Contributor License Agreement](https://cla.microsoft.com/) before submitting your pull request. To complete the Contributor License Agreement (CLA), you will need to submit a request via the [form](https://cla.microsoft.com/) and then electronically sign the CLA when you receive the email containing the link to the document. You need to sign the CLA only once to cover submission to any Microsoft OSS project. 

## 10. Contact
If you have further questions or are running into trouble that cannot be resolved by any of the steps here, feel free to open a Github issue here or contact us at mobilecentersdk@microsoft.com.
