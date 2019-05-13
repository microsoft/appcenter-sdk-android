# The following options are set by default.
# Make sure they are always set, even if the default proguard config changes.
-dontskipnonpubliclibraryclasses
-verbose

# These enums or models are accessed via reflection in common msal lib thus proguard removes them.
-keep public enum com.microsoft.identity.common.internal.ui.AuthorizationAgent {
    *;
}
-keep public enum com.microsoft.identity.common.internal.authorities.Environment {
    *;
}
-keep public enum com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter {
    *;
}
-keep public class com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters {
    *;
}
-keep public enum com.microsoft.identity.common.internal.request.SdkType {
    *;
}
-keep public class com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse {
    *;
}
-keep public class com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse {
    *;
}

# Classes for below package is used by MSAL library. Ignore warning coming from the dependency.
-dontwarn com.nimbusds.jose.**
