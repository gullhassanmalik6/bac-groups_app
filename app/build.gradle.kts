plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

import java.util.Properties

// Override in local.properties (not committed), e.g.:
// api.base.url=http://192.168.1.10:8000/api/v1/
// Until api.bacgroupsa.com DNS + Railway API are live, debug must use a reachable host.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val apiBaseUrlOverride: String? = localProps.getProperty("api.base.url")?.trim()?.takeIf { it.isNotEmpty() }
val productionApiBaseUrl = "https://api.bacgroupsa.com/api/v1/"
// Emulator → host machine. Physical device: set api.base.url in local.properties to your PC LAN IP.
val debugApiBaseUrl = apiBaseUrlOverride ?: "http://10.0.2.2:8000/api/v1/"

android {
    namespace = "com.cryptopos.pos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bacgroupsa.pos"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$productionApiBaseUrl\"")
        // Comma-separated sha256/ pins; empty disables pinning until ops provisions pins.
        // Example: "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        buildConfigField("String", "CERT_PINS", "\"\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"info@bacgroupsa.com\"")
        buildConfigField("boolean", "ENFORCE_DEVICE_INTEGRITY", "false")
        // Phase 6: call /terminal/* on API; fall back to on-device mock if unavailable.
        buildConfigField("boolean", "USE_REMOTE_TERMINAL", "true")
        // Phase 13: card-present processor key (mock_sandbox | certified_psp). Never invents live wire formats.
        buildConfigField("String", "PAYMENT_PROCESSOR", "\"mock_sandbox\"")
        buildConfigField("String", "PAYMENT_ENVIRONMENT", "\"SANDBOX\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Keep production hostname. Do not bake LAN overrides into release APKs.
            buildConfigField("String", "API_BASE_URL", "\"$productionApiBaseUrl\"")
            // Set real pins before production rollout; empty keeps DEFAULT pinner.
            buildConfigField("String", "CERT_PINS", "\"\"")
            buildConfigField("boolean", "ENFORCE_DEVICE_INTEGRITY", "true")
        }
        debug {
            // Do not point debug at api.bacgroupsa.com until DNS resolves — login fails with UnknownHost.
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("boolean", "ENFORCE_DEVICE_INTEGRITY", "false")
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
        compose = true
        buildConfig = true
        aidl = true
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.security.crypto)
    implementation(libs.biometric)
    implementation(libs.zxing.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
