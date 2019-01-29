# DeviceInfoActivity list Device properties dynamically like a bean
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.Device {
   public ** get*();
}

# Classes for below package is used by MSAL library. Ignore warning coming from the dependency.
-dontwarn com.nimbusds.jose.**

# TODO: Below config is temporary. Please remove this once we have binaries ready in jcenter.
-keep public class com.microsoft.appcenter.identity.Identity {
    *;
}