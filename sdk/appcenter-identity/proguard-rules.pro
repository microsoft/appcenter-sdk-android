# The following options are set by default.
# Make sure they are always set, even if the default proguard config changes.
-dontskipnonpubliclibraryclasses
-verbose
-keep public enum com.microsoft.identity.common.internal.ui.AuthorizationAgent  {
    *;
}