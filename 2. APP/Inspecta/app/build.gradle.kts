plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val cloudinaryCloudName = providers.gradleProperty("cloudinaryCloudName").orElse("").get()
val cloudinaryUploadPreset = providers.gradleProperty("cloudinaryUploadPreset").orElse("").get()
require(listOf(cloudinaryCloudName, cloudinaryUploadPreset).all { it.matches(Regex("[A-Za-z0-9_-]*")) }) {
    "O nome da conta e o preset do Cloudinary devem conter apenas letras, números, hífens ou sublinhados."
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.example.inspecta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.inspecta"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        exclude(group = "com.google.zxing", module = "core")
    }
    implementation("com.google.zxing:core:3.5.4")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
