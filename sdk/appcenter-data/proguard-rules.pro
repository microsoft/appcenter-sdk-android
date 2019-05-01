# The following options are set by default.
# Make sure they are always set, even if the default proguard config changes.
-dontskipnonpubliclibraryclasses
-verbose

# This enum is accessed via reflection in common msal lib thus proguard removes it.
-keep public enum com.microsoft.storage.common.internal.ui.AuthorizationAgent  {
    *;
}

# Classes for below package is used by MSAL library. Ignore warning coming from the dependency.
-dontwarn com.nimbusds.jose.**
