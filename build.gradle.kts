import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library") version "9.4.1"
    id("org.jetbrains.kotlin.android") version "2.4.20"
}

group = "com.nshiftui.rainbowparser"
version = "0.1.0-beta.1"

android {
    namespace = "com.nshiftui.rainbowparser"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:6.2.4")
    testImplementation("io.kotest:kotest-assertions-core:6.2.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}
