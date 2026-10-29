plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.openweb.browser"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "com.openweb.browser"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
    }

    signingConfigs {
        create("stable") {
            storeFile = file(System.getenv("KS_PATH"))
            storePassword = System.getenv("KS_PASS")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASS")
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stable")
        }
    }
}
\ndependencies {\n    implementation("androidx.credentials:credentials:1.5.0")\n    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")\n    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")\n}
