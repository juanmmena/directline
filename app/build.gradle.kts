plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// Official Google AdMob test IDs used for debug builds and safe CI fallback
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val testAdmobInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"

fun resolveAdmobProp(name: String, fallback: String): String =
  (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv(name)?.takeIf { it.isNotBlank() }
    ?: fallback

android {
  namespace = "com.directline"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.juanmanuel.directline"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug {
      manifestPlaceholders["admobAppId"] = testAdmobAppId
      buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$testAdmobBannerUnitId\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$testAdmobInterstitialUnitId\"")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      val releaseAppId = resolveAdmobProp("ADMOB_APP_ID", testAdmobAppId)
      val releaseBannerId = resolveAdmobProp("ADMOB_BANNER_AD_UNIT_ID", testAdmobBannerUnitId)
      val releaseInterstitialId = resolveAdmobProp("ADMOB_INTERSTITIAL_AD_UNIT_ID", testAdmobInterstitialUnitId)

      manifestPlaceholders["admobAppId"] = releaseAppId
      buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$releaseBannerId\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$releaseInterstitialId\"")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.kotlinx.coroutines.android)

  // Lifecycle & ViewModel
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose & Material 3
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Navigation
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)

  // Dependency Injection (Hilt)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Local Persistence (Room & DataStore)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)

  // Phone number parsing & validation
  implementation(libs.libphonenumber)

  // Google AdMob
  implementation(libs.google.play.services.ads)

  // Local unit tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)

  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
