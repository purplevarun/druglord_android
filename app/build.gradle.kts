import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing reads release.keystore and keystore.properties from the repo root.
// Both are gitignored. Where they are absent (CI, a fresh clone) the release APK is left unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.purplevarun.trader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.purplevarun.trader"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file("release.keystore")
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// The UI tests need the debug-only test activity, so unit tests run against debug only.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        (variant as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
}
