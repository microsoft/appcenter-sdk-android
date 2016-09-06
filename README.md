[![Build Status](https://www.bitrise.io/app/78891228f9c6e6dc.svg?token=KQ6kVAci490XBjulCcQuGQ&branch=master)](https://www.bitrise.io/app/78891228f9c6e6dc)
[![codecov](https://codecov.io/gh/Microsoft/Sonoma-SDK-Android/branch/master/graph/badge.svg?token=YwMZRPnYK3)](https://codecov.io/gh/Microsoft/Sonoma-SDK-Android)

# Sonoma SDK for Android

## Introduction

Android Sonoma SDK lets you add Sonoma services in your Android application.

The SDK is currently in private beta release and we support the following services:

1. Analytics: Sonoma Analytics helps you understand user behavior and customer engagement to improve your Android app. The SDK automatically captures session count, device properties like Model, OS Version etc. and pages. You can define your own custom events to measure things that matter
    to your business. All the information captured is available in Sonoma dashboard for you to analyze the data.
 
2. Error Reporting: Sonoma SDK will automatically collect crash log every time your app crashes. The log is first written to the device's storage and when the user starts the app again, the crash report will be forwarded to Sonoma. Collecting crashes works for both beta and live apps, i.e. those submitted to Google Play or other app stores. Crash logs contain viable information for you to help resolve the issue. The SDK lets you add a lot of power to a crash where a developer can collect and add additional information to the report if they'd like.

This document contains the following sections:

1. [Prerequisites](#prerequisites)
2. [Add Sonoma SDK modules](#add-sonoma-sdk-modules)
3. [Start the SDK](#start-the-sdk)
4. [Analytics APIs](#analytics-apis)
5. [Error Reporting APIs](#error-reporting-apis)
6. [Advanced APIs](#advanced-apis)
7. [Troubleshooting](#troubleshooting)
8. [List of available libraries](#list-of-available-libraries)


Let's get started with setting up Sonoma Android SDK in your app to use these services:

1. ### **Prerequisites** ###
    Before you begin, please make sure that the following prerequisites are met:

  * Android project that is set up in Android Studio.
  * Device running Android Version 4.0.3 with API level >= 15 or higher.

2. ### **Add Sonoma SDK modules** ###

    Sonoma SDK is designed with a modular approach where a developer needs to integrate only the modules of the services that interests them.

     Below are the steps on how to integrate our compiled libraries in your application using Android Studio and Gradle.

    * Open app level build.gradle file (app/build.gradle) and add the following lines after apply plugin. Since we are in private beta right now, you need to include credentials in order to get the libraries.
     
        apply plugin: 'com.android.application'
        ```json 
        repositories {
            maven {
                url  'http://microsoftsonoma.bintray.com/sonoma'
                credentials {
                    username 'sonoma-beta'
                    password 'cbaf881993ad1ab4128f71da716d060e8590906a'
                }
            }
        }
        ```

    * In the same file, include the dependencies that you want in your project. Each SDK module needs to be added as a separate dependency in this section. If you would want to use both Analytics and Errors, add the following lines:
        ```json 
        dependencies {
            compile 'com.microsoft.sonoma:analytics:+'
            compile 'com.microsoft.sonoma:errors:+'
        }
        ```

    * Save your build.gradle file and make sure to trigger a Gradle sync.

    Now that you've integrated the SDK in your application, it's time to start the SDK and make use of Sonoma services.
     
3. ### **Start the SDK** ### 
    To start the Sonoma SDK in your app, follow the steps:
    * **Get App Secret of your application:**   Before you call the API to start the SDK, you need to get your app specific Application Secret from the Sonoma portal that needs to be a part of the method call. This will make sure all the data collected by the SDK corresponds to your application. 

      Go over to the Sonoma portal, click on "Microsoft Azure Project Sonoma". Under "My apps", click on the app that you want the SDK to set up. Then click on "Manage app" and copy the "App Secret" to start the SDK.

    * **Start the SDK:**  Sonoma provides developers with two modules to get started- Analytics and Error Reporting. In order to use these modules, you need to opt in for the module that you'd like, meaning by default no modules are included and you will have to explicitly call each of them when starting the SDK.

        ```Java
        Sonoma.start(getApplication(), "<Your App Secret>", Analytics.class, ErrorReporting.class);
        ```
    The example above shows how to use start() method and include both Analytics and ErrorReporting module. If you wish not to use Analytics, remove the parameter from method call above. Note that unless you explicitly specify each module as parameters in the start method, you cannot use that Sonoma service. Also, start() API should be used only once in your app. Only the modules included in the first call would be available and all other calls will log a warning in the console.

4. ### **Analytics APIs** ###
    
    * **Track Session, Device Properties:**  Once Analytics module is included in your app and SDK is started, we automatically track sessions, device properties like OSVersion, Model, Manufacture etc. and you don’t need to add any line of code.
        Look at the section above on how to [Start the SDK](#start-the-sdk) if you haven't started yet.

    * **Custom Events:** You can track your own custom events with specific properties to know what's happening in your app, understand user actions and see the aggregates in Sonoma portal. Once you have started the SDK, use the trackEvent() method to track your events with properties.

        ```Java
        Map<String,String> properties = new HashMap<String, String>();
        properties.put("Category", "Music");
        properties.put("FileName", "favourite.avi");

        Analytics.trackEvent("Video clicked", properties);
        ```

    * **Enable or disable Analytics:**  You can disable and opt out of using Analytics module by calling setEnabled() API and the SDK will collect no Analytics information for your app. To enable again, pass "true" as a parameter in the same method.
       ```Java
        Analytics.setEnabled(false)
        ```

        You can also check if the module is enabled or not using isEnabled() method:
        ```Java
        Analytics.isEnabled()
        ```
    
5. ### **Error Reporting APIs** ###

    Once you set up and start Sonoma SDK to use Error Reporting module in your application, SDK will automatically start logging any crashes in the device's local storage. When the user opens the application again, crash log will be forwarded to Sonoma and you can analyze the crash along with the stack trace on the Sonoma dashboard. Follow the link to see how to [Start the SDK](#start-the-sdk) if you haven't already.

    * **Generate a test crash:**   We provide you with a static API to generate a test crash for easy testing of SDK. Note that this API can only be used in test/beta apps and won't work in production apps. 
      ```Java
      ErrorReporting.generateTestCrash()
      ```

    * **Details about the last crash:**   You can get the details about the crash that occurred in the last app session.
      ```Java
      ErrorReporting.getLastSessionErrorReport()
      ```

    * **Did the app crash in last session:**   Alternatively, if you'd like to check upon startup if the app crashed before, use the API below:
      ```Java
      ErrorReporting.hasCrashedInLastSession()
      ```

    * **Advanced Scenarios:**  Error Reporting module provides callback scenarios for developers to perform additional actions when sending crashes to Sonoma. You can add more power to your crash report by implementing any or all of these callback scenario. All these callbacks need to be implemented on ErrorReportingListener.   

        * **Should the crash be processed:**   Implement this callback if you'd like to decide if a particular crash needs to be processed or not. For example - there could be some system level crashes that you'd want to ignore and don't want to send to Sonoma.
        ```Java
            boolean ErrorReportingListener.shouldProcess(ErrorReport errorReport)
        ```

        * **User Confirmation:** If user privacy is important to you as a developer, you might want to get user confirmation before sending a crash to Sonoma. Our SDK exposes a callback where you can build your custom UI that prompts user to select one of these options - "Always Send", "Send". Based on the user input, the crash will be forwarded to Sonoma.
        ```Java
            boolean ErrorReportingListener.shouldAwaitUserConfirmation()
        ```

        Method API: 
        ```Java
            ErrorReporting.notifyUserConfirmation(int userConfirmation)

            userConfirmation should be one of SEND, DONT_SEND or ALWAYS_SEND
         ```

        * **Binary attachment:**  If you'd like to attach text/binary data to a crash report, implement this callback. Before sending the crash, our SDK will add the attachment to the crash report and you can view it on the Sonoma dashboard.   
        ```Java
            ErrorAttachment getErrorAttachment(ErrorReport errorReport)
         ```
        
        * **Before sending crash:**   This callback will be invoked just before the crash is sent to Sonoma.
            ```Java
            void ErrorReportingListener.onBeforeSending(ErrorReport errorReport);
            ```

        * **Crash sending succeeded:**   This callback will be invoked after sending crash reports succeeded.
            ```Java
            void ErrorReportingListener.onSendingFailed(ErrorReport errorReport, Exception e);
            ```

        * **Crash sending failed:**   This callback will be invoked after sending crash reports failed.
            ```Java
            void ErrorReportingListener.onSendingSucceeded(ErrorReport errorReport)
            ```
    * **Enable or disable Error Reporting module:**  You can disable and opt out of using ErrorReporting module by calling setEnabled() API and the SDK will collect no crashes for your app. Use the same API to enable again by passing "true" as a parameter.
        ```Java
        ErrorReporting.setEnabled(false)
        ```

        You can also check if the module is enabled or not using isEnabled() method:
        ```Java
        ErrorReporting.isEnabled()
        ```

6. ### **Advanced APIs** ###
    
    * **Debugging**: You can control the amount of log messages from Sonoma SDK that show up in LogCat. Use setLogLevel() API to enable additional logging while debugging. By default, it is set it to ASSERT.
     ```Java
        Sonoma.setLogLevel(Log.VERBOSE)
    ```

    * **Get Install Identifier**: Sonoma SDK creates an UUID for each device once the app is installed. This identifier remains same for a device when the app is updated and a new one is generated only when the app is re-installed. This API would be useful for debugging purpose.
    ```Java
        Sonoma.getInstallId()
    ``` 
    
    * **Enable/Disable Sonoma SDK:** If you want Sonoma SDK to be disabled completely, use setEnabled() API. Once used, our SDK will collect no information for any of the modules that were added.
    ```Java
        Sonoma.setEnabled(false)
    ``` 

7. ### **Troubleshooting** ###  
    * How long to wait for Analytics data to appear on the dashboard?

    * How long to wait for crashes to appear on the dashboard?

    * Do I need to include all the libraries? Is there anything included by default?  
      No, you can just include Sonoma modules that interests you. Once you integrate any module, Sonoma Core module will be included by default which contains logic for persistence, forwarding etc.

    * Can't see crashes on the dashboard
        * Check if the App Secret used to start the SDK matches the App Secret in Sonoma portal.

    * What data does SDK automatically collect for Analytics?

    * What permissions are required for the SDK?

    * Any privacy information tracked by SDK?



8. ### **List of available libraries** ###

     Gradle Dependency                       | Service          
     --------------------------------------- | ------------
     com.microsoft.sonoma:analytics:+    | Analytics    
     com.microsoft.sonoma:errors:+       | Error Reporting