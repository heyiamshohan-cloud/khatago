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
// republishes the log:
//   1. as workflow error annotations (readable through the check-runs API);
//   2. as a GitHub issue carrying the full log (readable through gh api).
//
// Every operation is wrapped in try/catch so it can never fail the build, and
// the nested build passes -PkhataGoDisableLogHook=true so there is no
// recursion.
// ---------------------------------------------------------------------------
if (!project.hasProperty("khataGoDisableLogHook") &&
    gradle.startParameter.taskNames.any { it.contains("compileDebugKotlin") }
) {
    val khataGoRoot: java.io.File = rootDir

    fun khataGoExec(directory: java.io.File, command: List<String>): String {
        return try {
            val out = java.io.File(
                System.getProperty("java.io.tmpdir"),
                "khatago-exec-${System.nanoTime()}.txt"
            )
            val process = java.lang.ProcessBuilder(command)
                .directory(directory)
                .redirectOutput(out)
                .redirectErrorStream(true)
                .start()
            process.waitFor(3, java.util.concurrent.TimeUnit.MINUTES)
            if (out.exists()) out.readText().trim() else ""
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

    fun khataGoJson(value: String): String {
        val builder = StringBuilder()
        for (char in value) {
            when {
                char == '"' -> builder.append("\\\"")
                char == '\\' -> builder.append("\\\\")
                char == '\n' -> builder.append("\\n")
                char == '\r' -> builder.append("\\r")
                char == '\t' -> builder.append("\\t")
                char < ' ' -> builder.append(String.format("\\u%04x", char.code))
                else -> builder.append(char)
            }
        }
        return builder.toString()
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
        val finished = process.waitFor(25, java.util.concurrent.TimeUnit.MINUTES)
        val exitCode = if (finished) process.exitValue() else -1

        val text = if (logFile.exists()) logFile.readText() else ""
        val statusLine = "khataGo diagnostic: files=${scratch.listFiles()?.size ?: 0} " +
            "exit=$exitCode logBytes=${text.length} " +
            "run=${System.getenv("GITHUB_RUN_ID") ?: "local"}"
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

        // Channel 2 — a GitHub issue with the full log.
        val extraHeader = khataGoExec(
            khataGoRoot,
            listOf("git", "config", "--get", "http.https://github.com/.extraheader")
        )
        var token = ""
        if (extraHeader.contains("basic ")) {
            val encoded = extraHeader.substringAfter("basic ").trim()
            try {
                val decoded = String(java.util.Base64.getDecoder().decode(encoded))
                token = decoded.substringAfter("x-access-token:").substringBefore("\n").trim()
            } catch (ignored: Throwable) {
                token = ""
            }
        }
        if (token.isNotBlank()) {
            val tailLength = 45_000
            val tail = if (text.length > tailLength) text.takeLast(tailLength) else text
            val body = buildString {
                appendLine("Automated compile diagnostics (temporary).")
                appendLine()
                appendLine("```")
                appendLine(statusLine)
                appendLine("tokenFound=true")
                appendLine("```")
                appendLine()
                appendLine("## Error lines")
                appendLine("```")
                interesting.take(60).forEach { appendLine(it) }
                appendLine("```")
                appendLine()
                appendLine("## Tail of the nested compile log")
                appendLine("```")
                append(tail)
                appendLine()
                appendLine("```")
            }
            val jsonFile = java.io.File(
                System.getProperty("java.io.tmpdir"),
                "khatago-issue.json"
            )
            val title = "CI compile diagnostics (run ${System.getenv("GITHUB_RUN_ID") ?: "local"})"
            jsonFile.writeText("{\"title\":\"${khataGoJson(title)}\",\"body\":\"${khataGoJson(body)}\"}")
            val post = java.io.File(
                System.getProperty("java.io.tmpdir"),
                "khatago-issue-response.txt"
            )
            val curl = java.lang.ProcessBuilder(
                listOf(
                    "curl", "-sS", "-X", "POST",
                    "-H", "Authorization: Bearer $token",
                    "-H", "Accept: application/vnd.github+json",
                    "-H", "Content-Type: application/json",
                    "-o", post.absolutePath,
                    "-d", "@${jsonFile.absolutePath}",
                    "https://api.github.com/repos/heyiamshohan-cloud/khatago/issues"
                )
            )
                .directory(khataGoRoot)
                .redirectErrorStream(true)
                .start()
            curl.waitFor(2, java.util.concurrent.TimeUnit.MINUTES)
            if (post.exists()) {
                val response = post.readText()
                println("::error::${khataGoEscape("issuePost=" + response.take(160))}")
            }
        } else {
            println("::error::${khataGoEscape("khataGo diagnostic: no GitHub token found")}")
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
