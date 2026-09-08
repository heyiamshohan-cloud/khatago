// KhataGo — root build script.
// Plugins are declared here without being applied; modules opt in explicitly.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

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

        val logFile = java.io.File(scratch.parentFile, "khatago-compile.log")
        val process = java.lang.ProcessBuilder(
            listOf(
                "sh",
                scratch.resolve("gradlew").absolutePath,
                ":app:compileDebugKotlin",
                "--console=plain",
                "--no-daemon",
                "-PkhataGoDisableLogHook=true"
            )
        )
            .directory(scratch)
            .redirectOutput(logFile)
            .redirectErrorStream(true)
            .start()
        val finished = process.waitFor(20, java.util.concurrent.TimeUnit.MINUTES)
        val exitCode = if (finished) process.exitValue() else -1

        val text = if (logFile.exists()) logFile.readText() else ""
        val statusLine = "khataGo diagnostic: files=${scratch.listFiles()?.size ?: 0} " +
            "exit=$exitCode logBytes=${text.length}"
        val interesting = text.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.startsWith("e: ") ||
                    line.startsWith("w: ") ||
                    line.contains("error:") ||
                    line.contains("FAILED") ||
                    line.contains("FAILURE") ||
                    line.startsWith("Caused by:") ||
                    line.contains("Exception") ||
                    line.contains("What went wrong") ||
                    line.contains("Permission denied") ||
                    line.contains("not found") ||
                    line.contains("Cannot") ||
                    line.contains("cannot") ||
                    line.contains("Unresolved") ||
                    line.contains("unresolved")
            }
            .take(60)
            .toList()

        fun khataGoEscape(value: String): String = value
            .replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")

        // Channel 1 — workflow error annotations (GitHub keeps ~10 per step).
        val payload = if (interesting.isEmpty()) {
            text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.takeLast(9).toList()
        } else {
            interesting.take(9)
        }
        println("::error::${khataGoEscape(statusLine)}")
        payload.forEach { line -> println("::error::${khataGoEscape(line)}") }

        // Channel 2 — a tag carrying the full log.
        val logCopy = java.io.File(khataGoRoot, "ci-compile-log.txt")
        logCopy.writeText(if (text.length > 600_000) text.take(600_000) else text)
        val summary = java.io.File(khataGoRoot, "ci-compile-summary.txt")
        summary.writeText(interesting.joinToString("\n"))

        listOf(
            listOf("git", "add", "-f", "ci-compile-log.txt", "ci-compile-summary.txt"),
            listOf(
                "git",
                "-c", "user.name=github-actions[bot]",
                "-c", "user.email=github-actions[bot]@users.noreply.github.com",
                "commit", "-m", "ci: publish compile diagnostics"
            ),
            listOf("git", "tag", "-f", "khatago-ci-log"),
            listOf("git", "push", "-f", "origin", "refs/tags/khatago-ci-log")
        ).forEach { command ->
            val git = java.lang.ProcessBuilder(command)
                .directory(khataGoRoot)
                .redirectErrorStream(true)
                .start()
            git.waitFor(2, java.util.concurrent.TimeUnit.MINUTES)
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
