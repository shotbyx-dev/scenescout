plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.scenescout.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.scenescout.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"

        // Google Maps API key: put MAPS_API_KEY=... in local.properties.
        // The map screen shows a graceful placeholder until a real key is set.
        val mapsKey: String = providers.gradleProperty("MAPS_API_KEY").getOrElse("MAPS_API_KEY_NOT_SET")
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
        // Free token from mapillary.com/dashboard — enables AI-eligible street imagery.
        val mlyToken: String = providers.gradleProperty("MAPILLARY_TOKEN").getOrElse("")
        buildConfigField("String", "MAPILLARY_TOKEN", "\"$mlyToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")

    // Maps + location (real map renders once MAPS_API_KEY is set)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // Image loading (spot galleries, map thumbnails)
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
