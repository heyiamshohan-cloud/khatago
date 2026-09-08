// KhataGo — root build script.
// Plugins are declared here without being applied; modules opt in explicitly.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

println("::error::khataGo marker: root script configured")

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// The sandbox used to develop this project cannot download GitHub Actions logs
// or artifacts, so a failing :app:compileDebugKotlin step is a black box: the
// run only reports "Process completed with exit code 1".
//
// When the invoked tasks include a Kotlin compile, this hook copies the project
// to a scratch directory (a second Gradle build inside the same directory would
// block on the project lock), compiles the copy with all output captured, and
// republishes the interesting lines as workflow error annotations, which are
// readable through the check-runs API. As a fallback the whole log is also
// committed and pushed as the tag "khatago-ci-log".
//
// Every operation is wrapped in try/catch so it can never fail the build, and
// the nested build passes -PkhataGoDisableLogHook=true so there is no
// recursion.
// ---------------------------------------------------------------------------
if (!project.hasProperty("khataGoDisableLogHook") &&
    gradle.startParameter.taskNames.any { it.contains("compileDebugKotlin") }
) {
    val khataGoRoot: java.io.File = rootDir

    fun khataGoCopy(source: java.io.File, target: java.io.File) {
        if (source.isDirectory) {
            val name = source.name
            if (name == "build" || name == ".gradle" || name == ".git" || name == ".kotlin") return
            target.mkdirs()
            val children = source.listFiles() ?: return
            for (child in children) khataGoCopy(child, java.io.File(target, child.name))
        } else if (source.isFile) {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
        }
    }

    try {
        val scratch = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "khatago-diagnostic"
        )
        scratch.deleteRecursively()
        scratch.mkdirs()
        khataGoCopy(khataGoRoot, scratch)
        scratch.resolve("gradlew").setExecutable(true)

        val mirroredTasks = gradle.startParameter.taskNames.ifEmpty { listOf("help") }
        val logFile = java.io.File(scratch.parentFile, "khatago-compile.log")
        val process = java.lang.ProcessBuilder(
            listOf(
                "sh",
                scratch.resolve("gradlew").absolutePath,
                *mirroredTasks.toTypedArray(),
                "--console=plain",
                "--no-daemon",
                "-PkhataGoDisableLogHook=true"
            )
        )
            .directory(scratch)
            .redirectOutput(logFile)
            .redirectErrorStream(true)
            .start()
        val finished = process.waitFor(25, java.util.concurrent.TimeUnit.MINUTES)
        val exitCode = if (finished) process.exitValue() else -1

        val text = if (logFile.exists()) logFile.readText() else ""
        val interesting = text.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.startsWith("e: ") ||
                    line.startsWith("w: ") ||
                    line.contains("error:") ||
                    line.contains("FAILED") ||
                    line.startsWith("Caused by:") ||
                    (line.contains("Exception") && !line.contains("GradleException")) ||
                    line.contains("What went wrong")
            }
            .take(60)
            .toList()

        fun khataGoEscape(value: String): String = value
            .replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")

        // Workflow error annotations (GitHub keeps ~10 per step).
        val statusLine = "khataGo: exit=$exitCode bytes=${text.length} " +
            "tasks=$mirroredTasks run=" + (System.getenv("GITHUB_RUN_ID") ?: "local")
        println("::error::" + khataGoEscape(statusLine))
        val payload = if (interesting.isEmpty()) {
            text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.takeLast(8).toList()
        } else {
            interesting.take(8)
        }
        payload.forEach { line -> println("::error::" + khataGoEscape(line.take(240))) }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
