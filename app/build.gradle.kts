plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

import java.util.Properties

android {
    namespace = "com.sumit.varnamala"
    compileSdk = 36

    val keystorePropertiesFile = rootProject.file("key.properties")
    val keystoreProperties = Properties()
    val hasKeystoreProperties = keystorePropertiesFile.exists()
    if (hasKeystoreProperties) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    signingConfigs {
        if (hasKeystoreProperties) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"]?.toString() ?: "")
                storePassword = keystoreProperties["storePassword"]?.toString()
                keyAlias = keystoreProperties["keyAlias"]?.toString()
                keyPassword = keystoreProperties["keyPassword"]?.toString()
            }
        }
    }

    defaultConfig {
        applicationId = "com.sumit.varnamala"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val isBundleTask = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }
    val enableApkSplits = !isBundleTask

    splits {
        abi {
            isEnable = enableApkSplits
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
    }

    val abiCodes = mapOf(
        "armeabi-v7a" to 1,
        "arm64-v8a" to 2,
        "x86" to 3,
        "x86_64" to 4
    )

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiFilter = output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                if (enableApkSplits && abiFilter != null) {
                    val abiCode = abiCodes[abiFilter.identifier] ?: 0
                    // Universal APK gets version code * 10, ABI-specific APKs get version code * 10 + abiCode
                    output.versionCode.set((output.versionCode.get() ?: 1) * 10 + abiCode)
                }
            }
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Official PaddleOCR PP-OCRv6 Android SDK (ONNX Runtime).
    implementation(project(":ppocr-sdk"))
    implementation(project(":transliteration"))

    // On-device Rasa TTS (same ONNX Runtime version as the OCR SDK, so one native library is packaged).
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.21.1")

    // Fingerprint / face / screen-lock prompt for the app lock (also brings in FragmentActivity).
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)

    // Background downloads
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    // Real org.json for JVM tests; the Android stub jar only throws.
    testImplementation("org.json:json:20231013")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
