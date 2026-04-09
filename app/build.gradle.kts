plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.daehyeon.dhon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.daehyeon.dhon"
        minSdk = 26
        targetSdk = 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    // ML Kit 번역
    implementation("com.google.mlkit:translate:17.0.3")

    // ML Kit OCR (명함 글자 인식용)
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    // 이미지 로딩 (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}