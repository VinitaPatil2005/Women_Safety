plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.women_safety"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.women_safety"
        minSdk = 26
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation (libs.androidx.fragment.ktx)
    implementation (libs.navigation.fragment.ktx.v277)
    implementation (libs.androidx.navigation.ui.ktx.v277)

    implementation (libs.androidx.core.ktx.v190)
    implementation (libs.androidx.appcompat.v161)
    implementation (libs.material)
    implementation (libs.androidx.constraintlayout)
    implementation (libs.play.services.location)

    // Navigation components
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)

    // Firebase Auth (for login/register)
    implementation (libs.firebase.auth.ktx)

    // Firebase Firestore (for user data)
    implementation (libs.firebase.firestore.ktx)



    implementation (libs.play.services.location.v2101)
    implementation (libs.androidx.core.ktx.v1120)
    implementation (libs.androidx.appcompat)
    implementation (libs.material.v190)
    implementation (libs.androidx.fragment.ktx.v161)
    implementation (libs.play.services.location)
}
//    implementation("com.google.gms.google-services")
