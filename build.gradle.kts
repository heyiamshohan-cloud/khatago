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
// other hand, are readable from the sandbox through the check-runs API, so this
// hook republishes what it can as annotations:
//
//   * on every build, the failure's exception chain ("why did this step fail");
//   * when a Kotlin compile was requested, the error lines from a nested build
//     of a scratch copy of the project (a second Gradle build inside the same
//     directory would block on the project lock).
//
// Everything is wrapped in try/catch so it can never fail the build, and the
// nested build passes -PkhataGoDisableLogHook=true so there is no recursion.
// ---------------------------------------------------------------------------
val khataGoRoot: java.io.File = rootDir

fun khataGoEscape(value: String): String {
    val builder = StringBuilder()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        val code = char.code
        if (code == 37) {
            builder.append("%25")
        } else if (code == 10) {
            builder.append("%0A")
        } else if (code == 13) {
            builder.append("%0D")
        } else if (code < 32) {
            // Drop other control characters; annotations are plain text.
        } else {
            builder.append(char)
        }
        index = index + 1
    }
    return builder.toString()
}

fun khataGoAnnotate(value: String) {
    val text = khataGoEscape(value)
    var offset = 0
    var emitted = 0
    while (offset < text.length && emitted < 9) {
        val end = kotlin.math.min(offset + 240, text.length)
        println("::error::" + text.substring(offset, end))
        offset = end
        emitted = emitted + 1
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

gradle.buildFinished { buildResult ->
    try {
        val failure = buildResult.failure
        if (failure == null) {
            println("::error::khataGo buildFinished: success")
        } else {
            val text = StringBuilder()
            var current: Throwable? = failure
            var depth = 0
            while (current != null && depth < 10 && text.length < 2000) {
                text.append(current.javaClass.simpleName)
                    .append(": ")
                    .append(current.message ?: "(no message)")
                    .append(" || ")
                current = current.cause
                depth = depth + 1
            }
            khataGoAnnotate("khataGo failure: " + text.toString())
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
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
            .take(9)
            .toList()
        val payload = if (interesting.isEmpty()) {
            text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.takeLast(8).toList()
        } else {
            interesting
        }
        khataGoAnnotate("khataGo nested: exit=$exitCode bytes=${text.length}")
        payload.forEach { line -> khataGoAnnotate("khataGo log: " + line) }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
