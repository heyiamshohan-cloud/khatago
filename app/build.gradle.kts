plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.shohan.khatago"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shohan.khatago"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += setOf("en")
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // Release builds fall back to the debug keystore when no signing config
        // is supplied by the environment (see RELEASE.md). No secrets are stored here.
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*"
            )
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        disable += setOf("GradleDependency", "OldTargetApi", "UnusedMaterial3ScaffoldPaddingParameter")
        baseline = file("lint-baseline.xml")
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    // Material Icons Extended: the outlined set used by KhataGo (AccountBalance,
    // BarChart, CalendarMonth, Person, ...) is not part of the core artifact.
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore, WorkManager
    implementation(libs.androidx.datastore.prefs)
    implementation(libs.androidx.work.runtime)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.sqlite.bundled)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// TEMPORARY CI DIAGNOSTIC — remove before release.
// Prints the full assertion message (expected vs actual) for failing unit tests,
// which is the only way to read them from the sandbox.
tasks.withType<Test> {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// Lint runs with abortOnError, and CI logs cannot be downloaded from the
// sandbox, so a failing lint step is otherwise unreadable. This finalizer runs
// even when lintDebug fails and republishes the lint errors as workflow
// annotations, which are readable through the check-runs API.
// ---------------------------------------------------------------------------
val khataGoLintReport = tasks.register("khataGoLintReport") {
    doLast {
        val xml = file("build/reports/lint-results-debug.xml")
        if (!xml.exists()) {
            println("::error::khataGo lint: no report at " + xml.path)
            return@doLast
        }
        val text = xml.readText()
        val messages = ArrayList<String>()
        for (chunk in text.split("<issue")) {
            if (!chunk.contains("severity=\"Error\"")) continue
            val message = java.util.regex.Pattern.compile("message=\"([^\"]*)\"").matcher(chunk)
            val location = java.util.regex.Pattern.compile("file=\"([^\"]*)\"").matcher(chunk)
            if (message.find()) {
                val where = if (location.find()) location.group(1).substringAfterLast('/') else "?"
                val id = java.util.regex.Pattern.compile("id=\"([^\"]*)\"").matcher(chunk)
                val issueId = if (id.find()) id.group(1) else "?"
                messages.add(where + " [" + issueId + "] " + message.group(1))
            }
        }
        println("::error::khataGo lint: " + messages.size + " error(s)")
        messages.take(8).forEach { entry ->
            val clean = entry
                .replace("%", "%25")
                .replace(13.toChar().toString(), "%0D")
                .replace(10.toChar().toString(), "%0A")
            println("::error::" + clean.take(230))
        }
    }
}

tasks.matching { it.name == "lintDebug" }.configureEach {
    finalizedBy(khataGoLintReport)
}
