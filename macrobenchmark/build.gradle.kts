plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.worktime.app.macrobenchmark"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        create("benchmark") {
            matchingFallbacks += listOf("release")
        }
    }

    // Macrobenchmark must run outside the target app process so it can stop, compile and
    // relaunch WorkTime between iterations.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
}
