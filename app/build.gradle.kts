plugins {
    alias(libs.plugins.android.application)
}

val tomtomApiKey: String by project

android {
    namespace = "com.example.moti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moti"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
        }

        debug {
            buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.osmdroid:osmdroid-android:6.1.1")

    implementation("org.slf4j:slf4j-android:1.7.36")

    implementation ("com.google.android.material:material:1.11.0")


    val version = "1.26.3"
    implementation("com.tomtom.sdk.maps:map-display:${version}")
    implementation("com.tomtom.sdk.location:provider-default:${version}")
    implementation("com.tomtom.sdk.location:provider-map-matched:${version}")
    implementation("com.tomtom.sdk.location:provider-simulation:${version}")
    implementation("com.tomtom.sdk.datamanagement:navigation-tile-store:${version}")
    implementation("com.tomtom.sdk.navigation:navigation-online:${version}")
    implementation("com.tomtom.sdk.navigation:ui:${version}")
    implementation("com.tomtom.sdk.routing:route-planner-online:${version}")
    implementation("com.tomtom.sdk.maps:map-display-api:${version}")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


    implementation ("com.google.android.gms:play-services-location:21.0.1")

}
