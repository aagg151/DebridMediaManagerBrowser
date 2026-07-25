plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.debrid.browser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.debrid.browser"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.6"
    }

    signingConfigs {
        create("release") {
            // Credentials are supplied at build time via environment variables so that
            // no keystore password is ever written to disk or committed to git.
            val storePath = System.getenv("RELEASE_STORE_FILE") ?: "release.keystore"
            val storePw = System.getenv("RELEASE_STORE_PASSWORD")
            val alias = System.getenv("RELEASE_KEY_ALIAS")
            val keyPw = System.getenv("RELEASE_KEY_PASSWORD")
            if (storePw != null && alias != null && keyPw != null) {
                storeFile = rootProject.file(storePath)
                storePassword = storePw
                keyAlias = alias
                keyPassword = keyPw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only attach the signing config when credentials were provided.
            if (System.getenv("RELEASE_STORE_PASSWORD") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.webkit:webkit:1.12.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Networking (Real-Debrid + TMDB APIs)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image loading (TMDB posters)
    implementation("io.coil-kt:coil:2.7.0")

    // Media3 / ExoPlayer (built-in player)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
}
