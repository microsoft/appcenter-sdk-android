# The following options are set by default.
# Make sure they are always set, even if the default proguard config changes.
-dontskipnonpubliclibraryclasses
-verbose

-keepclasseswithmembers class * extends com.microsoft.appcenter.distribute.download.ReleaseDownloader$Listener { *; }
