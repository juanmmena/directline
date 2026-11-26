# ProGuard / R8 rules for Directline

# Preserve libphonenumber metadata resources
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**
