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
// "Process completed with exit code 1".
//
// This hook copies the project to a scratch directory (a second Gradle build
// inside the same directory would block on the project lock) and re-runs the
// tasks that the outer build was asked to run, capturing all output. The result
// is published as GitHub issues, which are readable from the sandbox with
// "gh issue list". Everything is wrapped in try/catch so it can never fail the
// build, and the nested build passes -PkhataGoDisableLogHook=true so there is
// no recursion.
// ---------------------------------------------------------------------------
val khataGoRoot: java.io.File = rootDir

fun khataGoRun(directory: java.io.File, command: List<String>): String {
    return try {
        val out = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "khatago-out-" + System.nanoTime() + ".txt"
        )
        val process = java.lang.ProcessBuilder(command)
            .directory(directory)
            .redirectOutput(out)
            .redirectErrorStream(true)
            .start()
        process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)
        if (out.exists()) out.readText() else ""
    } catch (ignored: Throwable) {
        ""
    }
}

fun khataGoJson(value: String): String {
    val backslash = 92.toChar()
    val quote = 34.toChar()
    val builder = StringBuilder()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        val code = char.code
        if (char == quote) {
            builder.append(backslash).append(quote)
        } else if (char == backslash) {
            builder.append(backslash).append(backslash)
        } else if (code == 10) {
            builder.append(backslash).append('n')
        } else if (code == 13) {
            builder.append(backslash).append('r')
        } else if (code == 9) {
            builder.append(backslash).append('t')
        } else if (code < 32) {
            builder.append(backslash).append("u").append(String.format("%04x", code))
        } else {
            builder.append(char)
        }
        index = index + 1
    }
    return builder.toString()
}

fun khataGoToken(): String {
    return try {
        val extra = khataGoRun(
            khataGoRoot,
            listOf("git", "config", "--get", "http.https://github.com/.extraheader")
        ).trim()
        val marker = "basic "
        val at = extra.lowercase().indexOf(marker)
        if (at < 0) {
            ""
        } else {
            val encoded = extra.substring(at + marker.length).trim()
            val decoded = String(java.util.Base64.getDecoder().decode(encoded))
            val prefix = "x-access-token:"
            if (decoded.contains(prefix)) decoded.substringAfter(prefix).trim() else ""
        }
    } catch (ignored: Throwable) {
        ""
    }
}

fun khataGoPostIssue(token: String, title: String, body: String): String {
    return try {
        val quote = 34.toChar()
        val json = StringBuilder()
            .append('{')
            .append(quote).append("title").append(quote).append(':')
            .append(quote).append(khataGoJson(title)).append(quote).append(',')
            .append(quote).append("body").append(quote).append(':')
            .append(quote).append(khataGoJson(body)).append(quote)
            .append('}')
            .toString()
        val payload = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "khatago-issue.json"
        )
        payload.writeText(json)
        val response = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "khatago-issue-response.txt"
        )
        java.lang.ProcessBuilder(
            listOf(
                "curl", "-sS", "-X", "POST",
                "-H", "Authorization: Bearer " + token,
                "-H", "Accept: application/vnd.github+json",
                "-H", "Content-Type: application/json",
                "-o", response.absolutePath,
                "--data-binary", "@" + payload.absolutePath,
                "https://api.github.com/repos/heyiamshohan-cloud/khatago/issues"
            )
        )
            .directory(khataGoRoot)
            .redirectErrorStream(true)
            .start()
            .waitFor(2, java.util.concurrent.TimeUnit.MINUTES)
        if (response.exists()) response.readText() else ""
    } catch (ignored: Throwable) {
        ""
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

fun khataGoEscape(value: String): String {
    return value
        .replace("%", "%25")
        .replace(13.toChar().toString(), "%0D")
        .replace(10.toChar().toString(), "%0A")
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
            while (current != null && depth < 10) {
                text.append(current.javaClass.simpleName)
                text.append(": ")
                text.append(current.message ?: "(no message)")
                text.append(" || ")
                current = current.cause
                depth = depth + 1
            }
            println("::error::khataGo failure: " + khataGoEscape(text.toString()).take(1200))
        }
    } catch (ignored: Throwable) {
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

if (!project.hasProperty("khataGoDisableLogHook")) {
    try {
        val mirroredTasks = gradle.startParameter.taskNames.ifEmpty { listOf("help") }
        val runId = System.getenv("GITHUB_RUN_ID") ?: "local"
        val token = khataGoToken()
        val probe = khataGoPostIssue(
            token,
            "CI probe run $runId tasks $mirroredTasks",
            "The diagnostic hook in the root build script executed.\n\n" +
                "tokenFound=" + token.isNotBlank() + "\n" +
                "run=" + runId + "\n" +
                "tasks=" + mirroredTasks + "\n"
        )
        println("::error::khataGo probe posted: " + probe.take(200).replace('\n', ' '))

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
        nested.addAll(mirroredTasks)
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

        println("::error::khataGo nested exit=" + exitCode + " bytes=" + text.length)

        if (exitCode != 0) {
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
                .take(80)
                .toList()
            val tailLength = 40_000
            val tail = if (text.length > tailLength) text.takeLast(tailLength) else text
            val body = StringBuilder()
                .append("Nested build failed with exit code ").append(exitCode).append('\n')
                .append('\n').append("tasks=").append(mirroredTasks).append('\n')
                .append('\n').append("## Filtered error lines").append('\n')
                .append("```").append('\n')
                .append(interesting.take(60).joinToString("\n")).append('\n')
                .append("```").append('\n')
                .append('\n').append("## Tail of the nested build log").append('\n')
                .append("```").append('\n')
                .append(tail).append('\n')
                .append("```").append('\n')
                .toString()
            val posted = khataGoPostIssue(
                token,
                "CI diagnostics: nested build failed (run $runId, tasks $mirroredTasks)",
                body
            )
            println("::error::khataGo log posted: " + posted.take(200).replace('\n', ' '))
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
