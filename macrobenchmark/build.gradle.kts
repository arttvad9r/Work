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
            // The benchmark test APK is never distributed. Sign it with the standard debug
            // key so managed devices can install the instrumentation package locally/CI.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                    // Keep the current ATD image ABI explicit; AGP 10 changes the implicit
                    // default to arm64-v8a, which this x86_64 image cannot translate.
                    testedAbi = "x86_64"
                }
            }
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
