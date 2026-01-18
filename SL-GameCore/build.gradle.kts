
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.lanrhyme.shardlauncher.gamecore"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // Library modules do not support versionCode/versionName directly
        buildConfigField("int", "VERSION_CODE", "1221")
        buildConfigField("String", "VERSION_NAME", "\"a0.25.1221 - NEBULA\"")
        
        // Consumer proguard rules
        consumerProguardFiles("consumer-rules.pro")
    }
    
    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
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

    ndkVersion = "25.2.9519653"

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui") // For PointerIcon if needed, or check if specific
    implementation("androidx.compose.ui:ui-graphics")
    
    // Maven Artifact (for version comparison)
    implementation("org.apache.maven:maven-artifact:3.8.6")
    
    // DocumentFile
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.16.1")
    implementation("commons-codec:commons-codec:1.16.1")
    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("org.tukaani:xz:1.9")
    implementation("org.ow2.asm:asm-all:5.0.4")
    implementation("com.github.oshi:oshi-core:6.3.0")
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Native
    implementation("com.bytedance:bytehook:1.0.9")
    
    // OkHttp (if needed for basic utils, otherwise remove)
    implementation(libs.okhttp)
}
