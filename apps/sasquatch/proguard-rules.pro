# DeviceInfoActivity list Device properties dynamically like a bean
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.Device {
   public ** get*();
}

# TODO: Below config is temporary. Please remove this once we have binaries ready in jcenter.
-keep public class com.microsoft.appcenter.identity.Identity {
   *;
}