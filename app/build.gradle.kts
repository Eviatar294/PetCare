plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Apply Firebase plugin here
}

android {
    namespace = "com.example.petcare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.petcare"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}


dependencies {
    // Glide dependencies for Java (using annotationProcessor)
    implementation(libs.glide)  // Glide dependency
    annotationProcessor(libs.compiler) // Glide compiler for Java

    implementation(libs.firebase.storage)


    // Other dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.firebase.bom))     // Firebase BOM
    implementation(libs.firebase.database)          // Firebase Realtime Database
    implementation(libs.firebase.auth)              // Firebase Auth
    implementation(libs.material.v180)
}
