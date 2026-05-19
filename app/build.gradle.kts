import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("com.google.android.gms.oss-licenses-plugin")
}

// Conditionally apply Google Services plugin only for non-lite builds
if (project.findProperty("isLite") != "true") {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.cukbab"
    compileSdk = 36

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    val hasKeystore = keystorePropertiesFile.exists()

    if (hasKeystore) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.cukbab"
        minSdk = 30
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
        versionCode = 24
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ADMIN_EMAIL", "\"${keystoreProperties["adminEmail"] ?: ""}\"")
        buildConfigField("String", "REPORTER_BASE_URL", "\"${keystoreProperties["reporterBaseUrl"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${keystoreProperties["googleClientId"] ?: ""}\"")
    }

    flavorDimensions += "version"
    productFlavors {
        create("full") {
            dimension = "version"
            buildConfigField("Boolean", "SHOW_AUTH_FEATURES", "true")
            resValue("string", "app_name_flavor", "CUK밥")
        }
        create("lite") {
            dimension = "version"
            applicationIdSuffix = ".lite"
            versionNameSuffix = "-lite"
            buildConfigField("Boolean", "SHOW_AUTH_FEATURES", "false")
            resValue("string", "app_name_flavor", "CUK밥-lite")
        }
    }

    signingConfigs {
      if (hasKeystore) {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"]?.let { file(it) }
            storePassword = keystoreProperties["storePassword"] as String?
        }
      }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

          if(hasKeystore){
            signingConfig = signingConfigs.getByName("release")
          }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Fix for Gradle 9+ compatibility with OSS Licenses plugin
tasks.matching { it.name.contains("OssLicenses") }.configureEach {
    mustRunAfter(tasks.matching { it.name.contains("OssDependency") })
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.window)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.material)

    // Firebase & Auth (Full version only)
    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.firebase.auth)
    "fullImplementation"(libs.firebase.firestore)
    "fullImplementation"(libs.play.services.auth)
    "fullImplementation"(libs.androidx.credentials)
    "fullImplementation"(libs.androidx.credentials.play.services.auth)
    "fullImplementation"(libs.googleid)

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.scalars)
    implementation(libs.gson)

    // Widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.play.services.oss.licenses)
}
