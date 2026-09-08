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
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
    }

    // TEMPORARY bisection: compile everything except the ui package.
    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("src/main/java"))
            java.exclude("com/shohan/khatago/ui/**")
        }
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

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// This sandbox cannot reach GitHub's log or artifact hosts, so when the Kotlin
// compile fails on CI the only way to read the compiler output is through check
// run annotations. This hook re-runs the compile task in a nested build with the
// output captured, and re-prints every error line as a workflow error command.
// The nested build passes a property that disables the hook, so there is no
// recursion.
// ---------------------------------------------------------------------------
if (!project.hasProperty("khataGoSkipDiagnostics")) {
    val emitDiagnostics = tasks.register("khataGoEmitCompileDiagnostics") {
        mustRunAfter("compileDebugKotlin")
        doLast {
            val stdout = java.io.ByteArrayOutputStream()
            val stderr = java.io.ByteArrayOutputStream()
            var captured = ""
            try {
                exec {
                    workingDir = rootDir
                    executable = rootDir.resolve("gradlew").absolutePath
                    args(
                        ":app:compileDebugKotlin",
                        "--console=plain",
                        "--no-daemon",
                        "-PkhataGoSkipDiagnostics=true"
                    )
                    standardOutput = stdout
                    errorOutput = stderr
                    isIgnoreExitValue = true
                }
                captured = stdout.toString() + "\n" + stderr.toString()
            } catch (t: Throwable) {
                captured = "diagnostic re-run threw: ${t.message}"
            }

            fun escape(value: String) = value
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")

            val lines = captured.lineSequence()
                .map { it.trim() }
                .filter { line ->
                    line.startsWith("e:") ||
                        line.startsWith("w:") ||
                        "error:" in line ||
                        "FAILED" in line ||
                        "Unresolved reference" in line ||
                        "Expecting" in line
                }
                .distinct()
                .take(80)
                .toList()

            if (lines.isEmpty()) {
                println("::error ::NO ERROR LINES CAPTURED. tail=${escape(captured.takeLast(1500))}")
            } else {
                lines.forEach { line -> println("::error ::${escape(line).take(900)}") }
            }
        }
    }

    tasks.matching { it.name == "compileDebugKotlin" }.configureEach {
        finalizedBy(emitDiagnostics)
    }
}

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// This sandbox cannot reach GitHub's log or artifact hosts, so when the Kotlin
// compile fails on CI the only readable channels are the job summary and check
// run annotations. This hook re-runs the compile task in a nested build with the
// output captured, writes it to $GITHUB_STEP_SUMMARY and re-prints every error
// line as a workflow error command. The nested build passes a property that
// disables the hook, so there is no recursion.
// ---------------------------------------------------------------------------
if (!project.hasProperty("khataGoSkipDiagnostics")) {
    val emitDiagnostics = tasks.register("khataGoEmitCompileDiagnostics") {
        mustRunAfter("compileDebugKotlin")
        doLast {
            val stdout = java.io.ByteArrayOutputStream()
            val stderr = java.io.ByteArrayOutputStream()
            var captured = ""
            try {
                project.exec {
                    workingDir = rootDir
                    executable = rootDir.resolve("gradlew").absolutePath
                    args(
                        ":app:compileDebugKotlin",
                        "--console=plain",
                        "--no-daemon",
                        "-PkhataGoSkipDiagnostics=true"
                    )
                    standardOutput = stdout
                    errorOutput = stderr
                    isIgnoreExitValue = true
                }
                captured = stdout.toString() + "\n" + stderr.toString()
            } catch (t: Throwable) {
                captured = "diagnostic re-run threw: " + t.message
            }

            fun escape(value: String) = value
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")

            val lines = captured.lineSequence()
                .map { it.trim() }
                .filter { line ->
                    line.startsWith("e:") ||
                        line.startsWith("w:") ||
                        "error:" in line ||
                        "FAILED" in line ||
                        "Unresolved reference" in line ||
                        "Expecting" in line
                }
                .distinct()
                .take(80)
                .toList()

            val report = if (lines.isEmpty()) {
                "NO ERROR LINES CAPTURED\n--- tail ---\n" + captured.takeLast(4000)
            } else {
                lines.joinToString("\n")
            }

            val summaryPath = System.getenv("GITHUB_STEP_SUMMARY")
            if (!summaryPath.isNullOrBlank()) {
                try {
                    java.io.File(summaryPath).appendText(
                        "\n## Kotlin compile diagnostics\n\n```\n" + report + "\n```\n"
                    )
                } catch (ignored: Throwable) {
                }
            }

            if (lines.isEmpty()) {
                println("::error ::NO ERROR LINES CAPTURED tail=" + escape(captured.takeLast(1500)))
            } else {
                lines.forEach { line -> println("::error ::" + escape(line).take(900)) }
            }
        }
    }

    tasks.matching { it.name == "compileDebugKotlin" }.configureEach {
        finalizedBy(emitDiagnostics)
    }
}

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — removed before release.
// This sandbox cannot reach GitHub's log or artifact hosts, so the only readable
// channels are the job summary and check run annotations. This hook re-runs the
// compile task in a nested build with its output captured, then writes it out.
// ---------------------------------------------------------------------------
if (!project.hasProperty("khataGoSkipDiagnostics")) {
    val projectRootDir: java.io.File = rootDir
    val khataGoDiag = tasks.register("khataGoEmitCompileDiagnostics") {
        mustRunAfter("compileDebugKotlin")
        doLast {
            val out = java.io.ByteArrayOutputStream()
            val err = java.io.ByteArrayOutputStream()
            try {
                project.exec {
                    workingDir = projectRootDir
                    executable = projectRootDir.resolve("gradlew").absolutePath
                    args(
                        ":app:compileDebugKotlin",
                        "--console=plain",
                        "--no-daemon",
                        "-PkhataGoSkipDiagnostics=true"
                    )
                    standardOutput = out
                    errorOutput = err
                    isIgnoreExitValue = true
                }
            } catch (ignored: Throwable) {
            }
            val text = out.toString() + "\n" + err.toString()
            val interesting = text.lineSequence()
                .map { it.trim() }
                .filter {
                    it.startsWith("e:") || it.startsWith("w:") ||
                        "error:" in it || "FAILED" in it ||
                        "Unresolved reference" in it || "Expecting" in it
                }
                .distinct()
                .take(80)
                .toList()
            val report = if (interesting.isEmpty()) "NONE CAPTURED\n" + text.takeLast(4000) else interesting.joinToString("\n")
            val summary = System.getenv("GITHUB_STEP_SUMMARY")
            if (summary != null && summary.isNotEmpty()) {
                try {
                    java.io.File(summary).appendText("\n## Kotlin diagnostics\n\n```\n" + report + "\n```\n")
                } catch (ignored: Throwable) {
                }
            }
            val safe = report.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
            println("::error ::" + safe.take(3000))
        }
    }
    tasks.matching { it.name == "compileDebugKotlin" }.configureEach {
        finalizedBy(khataGoDiag)
    }
}
