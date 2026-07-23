// app/build.gradle
plugins {
    id "com.android.application"
    id "org.jetbrains.kotlin.android"
}

android {
    namespace "com.death.workshopdashboard"
    compileSdk 34

    defaultConfig {
        applicationId "com.death.workshopdashboard"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
        debug {
            minifyEnabled false
        }
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion "1.5.11"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation "androidx.core:core-ktx:1.13.1"
    implementation "androidx.activity:activity-compose:1.9.0"
    implementation "androidx.compose.ui:ui:1.6.8"
    implementation "androidx.compose.ui:ui-tooling-preview:1.6.8"
    implementation "androidx.compose.material3:material3:1.2.1"
    implementation "androidx.navigation:navigation-compose:2.7.7"

    debugImplementation "androidx.compose.ui:ui-tooling:1.6.8"
}// build.gradle (Project)
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:8.5.0"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}
