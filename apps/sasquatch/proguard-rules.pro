# DeviceInfoActivity list Device properties dynamically like a bean
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.Device {
   public ** get*();
}
-keepclasseswithmembers class com.microsoft.appcenter.analytics.EventProperties {
   ** getProperties();
}
-keepclasseswithmembers class com.microsoft.appcenter.analytics.PropertyConfigurator {
   private ** get*();
   private ** mEventProperties;
}
-keepclasseswithmembers class * extends com.microsoft.appcenter.ingestion.models.properties.TypedProperty {
   ** getValue();
}

# For some reason the previous rule doesn't work with primitive getValue return type
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty {
   public boolean getValue();
}
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty {
   public long getValue();
}
-keepclasseswithmembers class com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty {
   public double getValue();
}
