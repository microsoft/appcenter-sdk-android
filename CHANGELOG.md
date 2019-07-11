# App Center SDK for Android Change Log

## Version 2.2.1 (Under development)

___

## Version 2.2.0

### App Center

* **[Fix]** Remove unsecure UUID fallback when UUID generation theorically fails, in reality it never fails.
* **[Fix]** Check for running in App Center Test will now work when using AndroidX instead of the support library.
* **[Feature]** Add `AppCenter.isRunningInAppCenterTestCloud` to provide method to check if the application is running in Test Cloud.

### App Center Crashes

* **[Fix]** The in memory cache of error reports is now cleared when disabling Crashes.

### App Center Data

* **[Feature]** Add support for offline list of documents.
* **[Feature]** Change the default time-to-live (TTL) from 1 day to infinite (never expire).
* **[Fix]** Fix `isExpired` method in `LocalDocument` for incorrect handling of the `TimeToLive.INFINITE` value. 
* **[Feature]** Add `ReadOptions` parameter to the `list` API.
* **[Feature]** Serialize `null` document values.
* **[Fix]** Fix declaring `gson` as a build time dependency instead of runtime.
* **[Fix]** Allow null for `ReadOptions` and `WriteOptions` parameters.

___

## Version 2.1.0

### App Center

* **[Fix]** Handle incorrect usage of `AppCenter.setLogUrl` API to provide readable error message.
* **[Fix]** Fix decrypting values that have been stored for more than a year (such as the in-app update token).

### App Center Analytics

* **[Feature]** Support setting latency of sending events via `Analytics.setTransmissionInterval`.

### App Center Auth

* **[Feature]** Expose the ID Token and Access Token (as raw JWT format) in the `UserInformation` object returned from the sign-in method.
* **[Fix]** Fix missing proguard rules so that the app does not have to specify them.
* **[Fix]** Fix crash on silently refreshing token if initialization of MSAL fails.
* **[Fix]** Fix sign-in before start auth service never ends and blocks every next try.
* **[Breaking change]** The `UserInformation` class has been moved from the `appcenter` module to the `appcenter-auth` module and must now be imported as `import com.microsoft.appcenter.auth.UserInformation`.

### App Center Data

* **[Fix]** Fix an issue where invalid characters in the document ID are accepted at creation time but causing errors while trying to read or delete the document. The characters are `#`, `\`, `/`, `?`, and all whitespaces.

### App Center Crashes

* **[Fix]** Fix a crash that could sometimes occur while processing native crash reports.

### App Center Distribute

* **[Feature]** Add `Distribute.setEnabledForDebuggableBuild(boolean)` method to allow in-app updates in debuggable builds.
* **[Fix]** Fix duplicate in-app update dialog when restarting (or switching) activity quickly after clicking download. Also fixes a crash when choosing "Ask me in a day" in the duplicate dialog.
* **[Fix]** Fix a crash that could occur when downloading the update with a customized dialog and then calling `Distribute.notifyUserConfirmation(UpdateAction.POSTPONE)` right after calling `Distribute.notifyUserConfirmation(UpdateAction.UPDATE)`.
* **[Fix]** Fix a crash that could occur while trying to open the browser on some devices.

### App Center Push

* **[Fix]** Update Firebase dependency and AppCenter push logic to avoid a runtime issue with the latest Firebase messaging version 18.0.0.

___

## Version 2.0.0

Version 2 of the App Center SDK includes two new modules: Auth and Data.

### AppCenterAuth

 App Center Auth is a cloud-based identity management service that enables developers to authenticate application users and manage user identities. The service integrates with other parts of App Center, enabling developers to leverage the user identity to view user data in other services and even send push notifications to users instead of individual devices.

### AppCenterData

The App Center Data service provides functionality enabling developers to persist app data in the cloud in both online and offline scenarios. This enables you to store and manage both user-specific data as well as data shared between users and across platforms.

### AppCenterCrashes

* **[Feature]** After calling `Auth.signIn`, the next crashes are associated with an `accountId` corresponding to the signed in user. This is a different field than the `userId` set by `AppCenter.setUserId`. Calling `Auth.signOut` stops the `accountId` association for the next crashes.

### AppCenterDistribute

* **[Fix]** Fix in-app updates not working on new Samsung devices.

### AppCenterPush

* **[Feature]** After calling `Auth.signIn`, the push installation is associated to the signed in user with an `accountId` and can be pushed by using the `accountId` audience. This is a different field than the `userId` set by `AppCenter.setUserId`. The push installation is also updated on calling `Auth.signOut` to stop the association.
* **[Fix]** Fix updating push installation when setting or unsetting the user identifier by calling `AppCenter.setUserId`.

___

## Version 1.11.4

### AppCenter

* **[Fix]** Fix network connection state tracking issue, which prevented sending data in some restricted networks.
* **[Fix]** Fix possible deadlock on changing network connection state.

### AppCenterDistribute

* **[Fix]** Fix in-app updates not working on devices using Xiaomi MIUI from versions 10 and above.

___

## Version 1.11.3

### AppCenter

* **[Fix]** The SDK normally disables storing and sending logs when SQLite is failing instead of crashing the application. New SQLite APIs were introduced in version 1.9.0 and the new API exceptions were not caught, this is now fixed.

### AppCenterDistribute

* **[Fix]** Fix exception if we receive deep link intent with setup failure before `onStart`.
* **[Fix]** Fix checking updates for applications installed on corporate-owned single-use devices.

___

## Version 1.11.2

### AppCenter

* **[Fix]** Fix TLS 1.2 configuration for some specific devices running API level <21. The bug did not affect all devices running older API levels, only some models/brands, and prevented any data from being sent.

### AppCenterAnalytics

* **[Fix]** Extend the current session instead of starting a new session when sending events from the background. Sessions are also no longer started in background by sending an event or a log from another service such as push, as a consequence the push registration information will be missing from crash events information.

### AppCenterDistribute

* **[Fix]** Fix issue with forcing Chrome to open links when other browsers are the default.

___

## Version 1.11.0

### AppCenter

* **[Feature]** Allow users to set userId that applies to crashes, error and push logs. This feature adds an API, but is not yet supported on the App Center backend.
* **[Fix]** Do not delete old logs when trying to add a log larger than the maximum storage capacity.
* **[Fix]** Fix error detection of `setMaxStorageSize` API if database uses custom page size.
* **[Fix]** Fix minimum storage size verification to match minimum possible value.
* **[Fix]** Fix disabling logging of network state changes according to `AppCenter.getLogLevel`.
* **[Fix]** Fix logs duplication on unstable network.

### AppCenterCrashes

* **[Fix]** Fix a bug where crash data file could leak when the database is full.

### AppCenterPush

* **[Fix]** Fix push foreground listener after re-enabling push service.

___

## Version 1.10.0

### AppCenterAnalytics

* **[Feature]** Add API to specify event persistence priority.

### AppCenterCrashes

* **[Fix]** Preventing stack overflow crash while reading a huge throwable file.

___

## Version 1.9.0

### AppCenter

* **[Feature]** Add a `setMaxStorageSize` API which allows setting a maximum size limit on the local SQLite storage. Previously, up to 300 logs were stored of any size. The default value is 10MB.
* **[Security]** To enforce TLS 1.2 on all HTTPS connections the SDK makes, we are dropping support for API level 15 (which supports only TLS 1.0), the minimum SDK version thus becomes 16. Previous versions of the SDK were already using TLS 1.2 on API level 16+.
* **[Bug fix]** Fix validating and discarding `NaN` and infinite double values when calling `setCustomProperties`.

### AppCenterAnalytics

* **[Feature]** Add `pause`/`resume` APIs which pause/resume sending Analytics logs to App Center.
* **[Feature]** Add support for typed properties. Note that these APIs still convert properties back to strings on the App Center backend. More work is needed to store and display typed properties in the App Center portal. Using the new APIs now will enable future scenarios, but for now the behavior will be the same as it is for current event properties.
* **[Feature]** Preparation work for a future change in transmission protocol and endpoint for Analytics data. There is no impact on your current workflow when using App Center.

___

## Version 1.8.0

### AppCenterCrashes

* **[Fix]** Fix a bug where some initialize operations were executed twice.
* **[Fix]** Fix a bug where device information could be null when reading the error report client side.

### AppCenterDistribute

* **[Fix]** Fix a crash that could happen when starting the application.

### AppCenterAnalytics

* **[Feature]** Preparation work for a future change in transmission protocol and endpoint for Analytics data. There is no impact on your current workflow when using App Center.

___

## Version 1.7.0

### AppCenterAnalytics

- **[Feature]** Preparation work for a future change in transmission protocol and endpoint for Analytics data. There is no impact on your current workflow when using App Center.

### AppCenterPush

The Firebase messaging SDK is now a dependency of the App Center Push SDK to be able to support Android P and also prevent features to break after April 2019 based on [this announcement](https://firebase.googleblog.com/2018/04/time-to-upgrade-from-gcm-to-fcm.html).

You need to follow [some migration steps](https://docs.microsoft.com/en-us/appcenter/sdk/push/migration/android) after updating the SDK to actually use Firebase instead of the manual registration mechanism that we are providing. The non Firebase mechanism still works after updating the SDK but you will see a deprecation message, but this will not work on Android P devices until you migrate.

After updating the app to use Firebase, you will also no longer see duplicate notifications when uninstalling and reinstalling the app on the same device and user.

___

## Version 1.6.1

### AppCenter

- **[Feature]** Preparation work for a future change in transmission protocol and endpoint for Analytics data. There is no impact on your current workflow when using App Center.
- **[Improvement]** Enable TLS 1.2 on API levels where it's supported but not enabled by default (API level 16-19, this became a default starting API level 20). Please note we still support Android API level 15 and it uses TLS 1.0.
- **[Improvement]** Gzip is used over HTTPS when request size is larger than 1.4KB.
- **[Fix]** Fix a crash when disabling a module at the same time logs are sent.
- **[Fix]** Fix pretty print JSON in Android P when verbose logging is enabled.

### AppCenterCrashes

- **[Feature]** Enable reporting C/C++ crashes when [Google Breakpad](https://github.com/google/breakpad) is used in the application (Google Breakpad is not distributed by App Center). Please note that there is still work to be done to stabilize this feature in App Center. Stay tuned with our [Changelog](https://docs.microsoft.com/en-us/appcenter/general/changelog) to get updates on NDK crashes support.

___

## Version 1.5.1

### AppCenter

* **[Fix]** Fix a crash when network state changes at same time as SDK initializing.
* **[Fix]** Fix crashes when trying to detect we run on instrumented test environment.
* **[Fix]** Fix a deadlock when setting wrapper SDK information or setting log url while other channel operations performed such as when Crashes is starting.

### AppCenterCrashes

* **[Fix]** Fix reporting crash when process name cannot be determined.

### AppCenterPush

* **[Fix]** Fix notification text being truncated when large and now supports multi-line.

___

## Version 1.5.0

### AppCenterAnalytics

* **[Improvement]** Analytics now allows a maximum of 20 properties by event, each property key and value length can be up to 125 characters long.

### AppCenterPush

* **[Feature]** Configure default notification icon and color using meta-data.
* **[Fix]** Fixes the google.ttl field being considered custom data.
* **[Fix]** Fixes push notification not displayed if Google Play Services too old on the device.
* **[Fix]** Don't crash the application when invalid notification color is pushed.

___

## Version 1.4.0

### AppCenterDistribute

* **[Feature]** Add Session statistics for distribution group.

___

## Version 1.3.0

### AppCenterDistribute

* **[Feature]** Use tester app to enable in-app updates if it's installed.
* **[Feature]** Add reporting of downloads for in-app update.
* **[Improvement]** Add distribution group to all logs that are sent.

___

## Version 1.2.0

### AppCenter

* **[Fix]** Fix events association with crashes.
* **[Fix]** Fix network state detection.
* **[Fix]** Don't retry sending logs on HTTP error 429.
* **[Fix]** Some logs were not sent or discarded correctly on AppCenter enabled/disabled state changes.

### AppCenterCrashes

* **[Improvement]** Increase attachment file size limit from 1.5MB to 7MB.

### AppCenterPush

* **[Fix]** Fix a crash on Android 8.0 (exact version, this does not happen in 8.1) where having an adaptive icon (like launcher icon) would cause crash of the entire system U.I. on push. On Android 8.0 we replace the adaptive icon by a placeholder icon (1 black pixel) to avoid the crash, starting Android 8.1 the adaptive icon is displayed without fallback.

___

## Version 1.1.0

### AppCenter

* **[Feature]** SDK modules can be skipped from being started automatically without code modification during instrumented tests. The SDK now reads `APP_CENTER_DISABLE` variable from `InstrumentationRegistry.getArguments()` and will not start any module if the value is `All` or will just skip starting the services described by a **comma separated list** of the services to exclude from being started. Valid service names for the variable are `Analytics`, `Crashes`, `Distribute` or `Push`. The modules are always started if instrumentation context is not available (like when you build and launch your application normally).

### AppCenterCrashes

* **[Fix]** Fix a crash when sending an attachment larger than 1.4MB. The SDK is still unable to send large attachments in this release but now it does not crash anymore. An error log is printed instead.
* **[Improvement]** Allow wrapper SDKs such as Xamarin to report a managed exception (for example for .NET stack traces) while still saving the exception for client side report as Java Throwable (so the original exception can be read from client side after restart by using the SDK).

### AppCenterDistribute

* **[Improvement]** Updated translations.
* **[Improvement]** Users with app versions that still use Mobile Center can directly upgrade to versions that use this version of App Center, without the need to reinstall.

### AppCenterPush

* **[Improvement]** The Firebase SDK dependency is now optional. If Firebase SDK is not available at runtime, the push registers and generate notifications using only App Center SDK. The Firebase application and servers are still used whether the Firebase SDK is installed into the application or not.

  * The SDK is still compatible with `apply plugin: 'com.google.gms.google-services'` and `google-services.json`, but if you don't use Firebase besides App Center, you can replace that plugin and the json file by a call to `Push.setSenderId` before `AppCenter.start`. The **Sender ID** can be found on the **Cloud Messaging** tab of your Firebase console project settings (same place as the **Server Key**).
  * The SDK is still compatible with `"com.google.firebase:firebase-messaging:$version"` lines. But if you don't use Firebase besides App Center, you can now remove these dependencies.

___

## Version 1.0.0

### General Availability (GA) Announcement.

This version contains **breaking changes** due to the renaming from Mobile Center to App Center. In the unlikely event there was data on the device not sent prior to the update, that data will be discarded.

### AppCenter

* The SDK has been rebranded from Mobile Center to App Center. Please follow [the migration guide](https://review.docs.microsoft.com/en-us/appcenter/sdk/sdk-migration/android?branch=appcenter-ga) to update from an earlier version of Mobile Center SDK.

### AppCenterDistribute

* **[Fix]** The view release notes button was not correctly hidden when no release notes were available.
* **[Fix]** Added missing translations for failed to enable in-app update dialog title and buttons. The main message however is not localized yet as it's extracted from a REST API text response.
* **[Known issue]** When updating an application that uses Mobile Center SDK using the in-app update dialog to an application version that uses AppCenter SDK version, the browser will be re-opened and will fail. User will need to re-install the application from the App Center portal.

___

## Version 0.13.0

* Localize in-app update texts, see [this folder](https://github.com/Microsoft/mobile-center-sdk-android/tree/develop/sdk/mobile-center-distribute/src/main/res) for a list of supported languages.
* When in-app updates are disabled because of side-loading, a new dialog will inform user instead of being stuck on a web page. Dialog actions offer ignoring in-app updates or following a link to re-install from the portal. This new dialog has texts that are not localized yet.
* Fix a bug where a failed version check could trigger reopening the browser in failure to enable in-app updates.
* Add `MobileCenter.getSdkVersion()` API to check Mobile Center SDK version at runtime.

___

## Version 0.12.0

- **[Feature]** New feature that allows to share your applications to anyone with public link.

- **[MISC]** When you update to this release, there will be **potential data loss** if an application installed with previous versions of MobileCenter SDK on devices that has pending logs which are not sent to server yet at the time of the application is being updated.

___

## Version 0.11.2

* Truncate event name and properties automatically instead of skipping them.
* Russian localization for in-app update texts.

___

## Version 0.11.1

Fix a regression in in-app updates from [version 0.11.0](https://github.com/Microsoft/mobile-center-sdk-android/releases/tag/0.11.0) where we could show unknown sources dialog on Android 8 if targeting older versions and unknown sources enabled.

Actually in that scenario, we can't detect if unknown sources are enabled and will just skip that dialog, system dialog will be shown at install time instead.

___

## Version 0.11.0

### Strict mode

This release focuses on fixing strict mode issues (including Android 8 ones).

Since strict mode checks if you spend time reading storage on U.I. thread we had to make the following APIs asynchronous and is thus a **breaking change**:

* `{AnyClass}.isEnabled()`
* `MobileCenter.getInstallId()`
* `Crashes.hasCrashedInLastSession()`

Those APIs returns a `MobileCenterFuture` object that is used to monitor the result, you can either use `get()` or `thenAccept(MobileCenterConsumer)` to either block or get the result via callback.

For symmetry purpose, `{AnyClass}.setEnabled(boolean)` also return a `MobileCenterFuture` object but most users don't need to monitor the result of the operation (consistency of calls sequence is guaranteed even if you don't wait for the change to be persisted).

Also `Crashes.getLastSessionCrashReport` was already asynchronous but signature changed to use the new `MobileCenterFuture` object.

`MobileCenterFuture` is similar to Java 8 `CompletableFuture` but works on Java 7 on any API level and has limited number of methods and does not throw exceptions (and executes the `thenAccept` callback in the U.I. thread).

### Other changes

* Fix a bug on Android 8 where network state could be detected as disconnected while network is available.
* Fix showing unknown sources warning dialog on Distribute on Android 8.
* Update Firebase SDK dependencies in Push to 11.0.2 to avoid conflict with Firebase recent getting started instructions.
* Update internal crash code to make it more compatible with Xamarin.Android and possibly future wrapper SDKs.

___

## Version 0.10.0

* Add `MobileCenter.setCustomProperties` API to segment audiences.
* Fix push and distribute notifications on Android 8 when targeting API level 26.
* Add a new method `Push.checkLaunchedFromNotification` to use in `onNewIntent` if `launchMode` of the activity is not standard to fix push listener when clicking on background push and recycling the activity.
* Fix crashing when calling `{AnyService}.isEnabled()` / or `setEnabled` before `MobileCenter.start`, now always return false before start.
* Fix a bug where 2 sessions could be reported at once when resuming from background.

___

## Version 0.9.0

Add `getErrorAttachments` callback to CrashesListener.

___

## Version 0.8.1

Fix a memory leak in HTTP client.

___

## Version 0.8.0

* Add network state debug logs.
* Add push module relying on Firebase to push notifications to the users of your application.

___

## Version 0.7.0

This version contains bug fixes, improvements and new features.

### Analytics

* **[Misc]** Events have some validation and you will see the following in logs:

    * An error if the event name is null, empty or longer than 256 characters (event is not sent in that case).
    * A warning for invalid event properties (the event is sent but without invalid properties):

       * More than 5 properties per event (in that case we send only 5 of them and log warnings).
       * Property key null, empty or longer than 64 characters.
       * Property value null or longer than 64 characters.

### Distribute

* **[Feature]** New Distribute listener to provide an ability of in-app update customization.
* **[Feature]** New default update dialog with release notes view.
* **[Bug fix]** Fix a crash when failing to download a mandatory update while showing progress dialog.
* **[Bug fix]** Fix duplicate update dialog when a new release is detected after restarting and processing the current update.

___

## Version 0.6.1

- **[Bug fix]** Fix a crash that could happen when application going to background while progress dialog on mandatory update download was displayed.
- **[Bug fix]** Fix a bug where progress dialog could be stuck at 100%.
- **[Improvement]** Offline cache for update dialog.

___

## Version 0.6.0

- **[Feature]** New service called `Distribute` to enable in-app updates for your Mobile Center builds.
- **[Improvement]** The improvement to wait up to 5 seconds for flushing logs to storage on crash is now active even if the Crashes feature is not used.
- **[Bug fix]** `401` HTTP errors (caused by invalid `appSecret`) were retried as a recoverable error. They are now considered unrecoverable.
- **[Misc]** A new log is sent to server when `MobileCenter` is started with the list of `MobileCenter` services that are used in the application.
- **[Misc]** Renamed `setServerUrl` to `setLogUrl`.

___

## Version 0.5.0

**Breaking change**: Remove Crashes APIs related to attachments as it's not supported by backend yet.

___

## Version 0.4.0

- Most of the crash processing is now happening on a background thread when the application restarts, solving multiple strict mode policy issues.
  - As a consequence, the `getLastSessionCrashReport` is now an asynchronous function with a callback (**breaking change**).
- Fix Proguard configuration inside the main AAR file, no Proguard configuration required on application side.
- Fix a race condition crash inside HTTP channel management when counting pending logs.
- Fix other race conditions in HTTP channel management that could lead to inconsistent behavior.
- Fix crash when the default ASyncTask thread pool is saturated when sending HTTP logs (now retries later).
- `StackOverflowError` throwable object for client side inspection is now truncated to `256` frames.
- App secret is now obfuscated in logs when setting verbose log level.
- Threads where crash callbacks are executed is now documented with the support annotations and the behavior is now consistent (either always UI thread or always worker thread depending on the callback).

___

## Version 0.3.3

- Fix a bug where `CrashesListener.onBeforeSending` or `CrashesListener.onSendingFailed` could not be called.
- Truncate `StackOverFlowError` to 256 frames (128 at start and 128 at end) to allow them being reported.
- Retry more https errors due to transient ssl failures when the client or server connectivity is bad.
- On crash, wait 5 seconds for local storage to flush other pending events to disk so that they are not lost.

___

## Version 0.3.2

- Fix empty text attachment being sent along with a binary only attachment.
- Fix logs when enabling or disabling MobileCenter.
- Fix debug logs for user confirmation callbacks.
- Add possibility to add raw stack trace for Xamarin.

___

## Version 0.3.1

- Fix/improve SDK logging.
- Don't validate appSecret against UUID format anymore (server validates appSecret).
- Allow wrapper SDK such as Xamarin to store additional crash data file.

___

## Version 0.3.0

- Rename `initialize` to `configure` in `MobileCenter` methods and logs.
- Fix null pointer exception when disabling a service during an HTTP call.

___

## Version 0.2.0

- Rename Sonoma to MobileCenter.
- Fix a bug that caused crashes to be sent twice if calling Crashes.setEnabled(true) while already enabled.
- New logs in assert level for successful SDK initialization or failed to initialize.
- Allow wrapper SDKs such as Xamarin to label an exception as being generated by the wrapper SDK.

___

## Version 0.1.4

- Remove trackPage/trackException from public APIs and disable automatic page tracking.
- Add assert level logs and a new NONE constant to disable all logs included assert ones.

___

## Version 0.1.3

Refactoring to solve Xamarin bindings issues.

___

## Version 0.1.2

- start_session is sent without sending any page or event
- start_session is sent immediately if re-enabling analytics module while in foreground (only if core was enabled though)

___

## Version 0.1.1

- Calls to SQLite are now asynchronous.
- Fix corner cases in new session detection, especially for single activity applications.

___

## Version 0.1.0

First release of the Sonoma SDK for Android.
