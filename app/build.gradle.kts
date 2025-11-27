plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.moti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moti"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 👇 Add this to resolve duplicate META-INF files
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.osmdroid:osmdroid-android:6.1.1")

    // GraphHopper core (includes OSM reader functionality)
    implementation("com.graphhopper:graphhopper-core:7.0")

    implementation("org.slf4j:slf4j-android:1.7.36")
}
