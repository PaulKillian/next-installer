plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.example.nextinstaller"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.nextinstaller"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.06.00"))
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("androidx.browser:browser:1.7.0")
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.webkit:webkit:1.11.0")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.compose.ui:ui-tooling-preview")
}
