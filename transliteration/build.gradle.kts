plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Pure Kotlin, no platform APIs: the same engine serves Android (through the JVM target)
// and can be built for desktop, iOS, and the web without changes.
kotlin {
    jvm {
        // Matches the Android modules, which compile for Java 11.
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    }
    js {
        browser()
        nodejs()
    }
    // iOS native targets are commented out under Gradle 9 until KGP adds Gradle 9 support for Kotlin/Native artifact publication
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.junit)
        }
    }
}
