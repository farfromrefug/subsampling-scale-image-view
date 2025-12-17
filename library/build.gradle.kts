import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "com.davemorrissey.labs.subscaleview"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("proguard-rules.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
//    api(libs.image.decoder)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.tachiyomiorg"
                artifactId = "subsampling-scale-image-view"
                version = "4.0.0"

                from(components["release"])
            }
        }

        repositories {
            maven {
                name = "jitpack"
                url = uri("https://jitpack.io")
            }
        }
    }
}
