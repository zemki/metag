import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    cocoapods {
        version = "1.0"
        summary = "MeTag Shared Module"
        homepage = "https://github.com/metagapp"
        ios.deploymentTarget = "14.0"

        framework {
            baseName = "shared"
            isStatic = true
        }

        // Note: Firebase is used in Swift code via iOS app's Podfile,
        // not in Kotlin shared module
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kmp.date.time.picker)
            implementation(libs.kermit)

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.security.crypto)
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Firebase for push notifications
            implementation("com.google.firebase:firebase-messaging:24.1.0")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }


}

android {
    namespace = "de.zemki.metagcompose"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        compose = true
    }

}

compose.resources {
    publicResClass = false
    packageOfResClass = "de.zemki.metagcompose.resources"
    generateResClass = auto
}
