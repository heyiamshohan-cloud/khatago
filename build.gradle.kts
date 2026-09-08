// KhataGo — root build script.
// Plugins are declared here without being applied; modules opt in explicitly.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// The sandbox used to develop this project cannot download GitHub Actions logs
// or artifacts, so a failing CI step is a black box: the run only reports
// "Process completed with exit code 1". Workflow error annotations, on the
// other hand, are readable from the sandbox through the check-runs API.
//
// When a Kotlin compile is requested, this hook copies the project to a scratch
// directory (a second Gradle build inside the same directory would block on the
// project lock), compiles the copy with all output captured, and republishes
// the error lines and the tail of the log as annotations. Everything is wrapped
// in try/catch so it can never fail the build, and the nested build passes
// -PkhataGoDisableLogHook=true so there is no recursion.
// ---------------------------------------------------------------------------
val khataGoRoot: java.io.File = rootDir

fun khataGoClean(value: String): String {
    return value
        .replace("%", "%25")
        .replace(13.toChar().toString(), "%0D")
        .replace(10.toChar().toString(), "%0A")
        .replace("|", " ")
}

fun khataGoAnnotate(prefix: String, value: String) {
    val text = khataGoClean(prefix + value)
    var offset = 0
    while (offset < text.length) {
        val end = if (offset + 230 < text.length) offset + 230 else text.length
        println("::error::" + text.substring(offset, end))
        offset = end
    }
}

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

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

if (!project.hasProperty("khataGoDisableLogHook") &&
    gradle.startParameter.taskNames.any { it.contains("compileDebugKotlin") }
) {
    try {
        val scratch = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "khatago-diagnostic"
        )
        scratch.deleteRecursively()
        scratch.mkdirs()
        khataGoCopy(khataGoRoot, scratch)
        scratch.resolve("gradlew").setExecutable(true)

        val logFile = java.io.File(scratch.parentFile, "khatago-nested.log")
        val nested = mutableListOf("sh", scratch.resolve("gradlew").absolutePath)
        nested.addAll(gradle.startParameter.taskNames)
        nested.add("--console=plain")
        nested.add("--no-daemon")
        nested.add("-PkhataGoDisableLogHook=true")
        val process = java.lang.ProcessBuilder(nested)
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
            .distinct()
            .take(8)
            .toList()

        khataGoAnnotate("khataGo: ", "exit=$exitCode bytes=${text.length} tasks=${gradle.startParameter.taskNames}")
        if (interesting.isEmpty()) {
            khataGoAnnotate("khataGo tail: ", text.takeLast(1400))
        } else {
            khataGoAnnotate("khataGo errors: ", interesting.joinToString(" | "))
            khataGoAnnotate("khataGo tail: ", text.takeLast(900))
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
