import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

apply(from = "conventions.gradle.kts")

val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun secret(name: String): String? =
    (providers.environmentVariable(name).orNull ?: secrets.getProperty(name))?.takeIf { it.isNotBlank() }

android {
    namespace = "com.example.mobilestack"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.example.mobilestack"
        minSdk = 26
        targetSdk = 36
        versionName = providers.gradleProperty("appVersionName").get()
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)

        manifestPlaceholders["urlScheme"] = "mobilestack"
        manifestPlaceholders["universalLinkHost"] = "mobilestack.example.com"
        buildConfigField("String", "URL_SCHEME", "\"mobilestack\"")
        buildConfigField("String", "UNIVERSAL_LINK_HOSTS", "\"mobilestack.example.com\"")
        buildConfigField("String", "ANALYTICS_API_KEY", "\"${secret("ANALYTICS_API_KEY").orEmpty()}\"")
        buildConfigField("String", "ERROR_REPORTING_DSN", "\"${secret("ERROR_REPORTING_DSN").orEmpty()}\"")
    }

    signingConfigs {
        // One debug key for the whole team: SHA-bound services (sign-in, App Links) work on every machine.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        secret("RELEASE_STORE_FILE")?.let { path ->
            create("release") {
                storeFile = rootProject.file(path)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "MobileStack Dev")
            buildConfigField("String", "APP_ENVIRONMENT", "\"development\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.com\"")
            buildConfigField("boolean", "USE_FIXTURES", "true")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "MobileStack Staging")
            buildConfigField("String", "APP_ENVIRONMENT", "\"staging\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.com\"")
            buildConfigField("boolean", "USE_FIXTURES", "true")
        }
        create("production") {
            dimension = "environment"
            resValue("string", "app_name", "MobileStack")
            buildConfigField("String", "APP_ENVIRONMENT", "\"production\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
            buildConfigField("boolean", "USE_FIXTURES", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        // The same scenario files drive the iOS app and the behavior tests on both platforms.
        getByName("main").assets.srcDir(rootProject.file("../../packages/contracts/fixtures"))
    }

    // Editor tooling (Compose Preview, kotlin-lsp project import) refuses to configure without this.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(project(":core"))
    // The app never calls introspection; only the debug-only socket server does.
    debugImplementation(project(":introspection"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    debugImplementation(libs.compose.ui.tooling)
}
