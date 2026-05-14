plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.nr"   // 🔥 FIX: match your actual package
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nr"   // 🔥 FIX: match package
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material
    implementation("androidx.compose.material3:material3")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 🔥 IMPORTANT (fix crash with Flow + Compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // ROOM
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

// Firebase Auth
    implementation("com.google.firebase:firebase-auth-ktx")

// Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("com.google.maps.android:maps-compose:4.3.3")

    implementation("com.google.android.gms:play-services-maps:19.0.0")


}
