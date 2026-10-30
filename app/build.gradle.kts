plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Release signing comes from Gradle properties or the environment, never from the repo. */
fun secret(name: String): String? = (findProperty(name) as String?)?.ifBlank { null } ?: System.getenv(name)?.ifBlank { null }

val uploadStoreFile = secret("QUIET_UPLOAD_STORE_FILE")

// Google's public AdMob test IDs. They always serve test ads and are safe to tap.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestRewardedUnit = "ca-app-pub-3940256099942544/5224354917"
// The real IDs, for Play builds only. See docs/releasing.md.
val releaseAdmobAppId = secret("QUIET_ADMOB_APP_ID")
val releaseSkipWaitUnit = secret("QUIET_ADMOB_SKIP_WAIT_UNIT")
val releaseHasRealAds = releaseAdmobAppId != null && releaseSkipWaitUnit != null

android {
    namespace = "app.quiet"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.quiet"
        minSdk = 26
        targetSdk = 36
        versionCode = 210
        versionName = "2.1.0"
    }

    signingConfigs {
        // A fixed debug key checked into the repo, so CI builds can be installed over each other
        // without uninstalling (which would wipe stats and settings).
        getByName("debug") {
            storeFile = rootProject.file("app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The Play upload key. See docs/releasing.md.
        if (uploadStoreFile != null) {
            create("release") {
                storeFile = file(uploadStoreFile)
                storePassword = secret("QUIET_UPLOAD_STORE_PASSWORD")
                keyAlias = secret("QUIET_UPLOAD_KEY_ALIAS")
                keyPassword = secret("QUIET_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField("String", "REWARDED_SKIP_WAIT_UNIT", "\"$admobTestRewardedUnit\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (uploadStoreFile != null) signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["admobAppId"] = if (releaseHasRealAds) releaseAdmobAppId!! else admobTestAppId
            buildConfigField("String", "REWARDED_SKIP_WAIT_UNIT", "\"${if (releaseHasRealAds) releaseSkipWaitUnit else admobTestRewardedUnit}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

if (!releaseHasRealAds) {
    gradle.taskGraph.whenReady {
        val packagingRelease = allTasks.any { it.name == "bundleRelease" || it.name == "assembleRelease" }
        if (packagingRelease && uploadStoreFile != null) {
            // A signed build is one you could upload, so never let it go out with test ads.
            throw GradleException("Quiet: the upload key is set but the real AdMob IDs aren't. See docs/releasing.md.")
        }
        if (packagingRelease) {
            logger.warn("Quiet: real AdMob IDs aren't set, so this release build shows test ads. See docs/releasing.md.")
        }
    }
}
