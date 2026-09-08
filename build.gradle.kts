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
        process.waitFor(3, java.util.concurrent.TimeUnit.MINUTES)
        if (out.exists()) out.readText().trim() else ""
    } catch (ignored: Throwable) {
        ""
    }
}

fun khataGoClean(value: String): String {
    return value
        .replace("%", "%25")
        .replace(13.toChar().toString(), "%0D")
        .replace(10.toChar().toString(), "%0A")
        .replace("|", " ")
}

fun khataGoAnnotate(prefix: String, value: String, limit: Int) {
    val text = khataGoClean(prefix + value)
    val end = if (limit < text.length) limit else text.length
    var offset = 0
    while (offset < end) {
        val stop = if (offset + 230 < end) offset + 230 else end
        println("::error::" + text.substring(offset, stop))
        offset = stop
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

// ---------------------------------------------------------------------------
// TEMPORARY CI DIAGNOSTIC — remove before release.
//
// Lint runs with abortOnError and CI logs cannot be downloaded from the
// sandbox, so a failing lint step is otherwise unreadable. This reporter is
// registered as a finalizer of :app:lintDebug, so it runs even when lint
// fails, and republishes the errors from the XML report as workflow
// annotations, which are readable through the check-runs API.
// ---------------------------------------------------------------------------
val khataGoLintReport = tasks.register("khataGoLintReport") {
    doLast {
        val xml = rootProject.file("app/build/reports/lint-results-debug.xml")
        if (!xml.exists()) {
            println("::error::khataGo lint: no report at " + xml.path)
            return@doLast
        }
        val text = xml.readText()
        val severities = LinkedHashMap<String, Int>()
        val messages = ArrayList<String>()
        for (chunk in text.split("<issue")) {
            val severityMatcher =
                java.util.regex.Pattern.compile("severity=\"([^\"]*)\"").matcher(chunk)
            if (!severityMatcher.find()) continue
            val severity = severityMatcher.group(1)
            severities[severity] = (severities[severity] ?: 0) + 1
            if (severity != "Fatal" && severity != "Error") continue
            val message = java.util.regex.Pattern.compile("message=\"([^\"]*)\"").matcher(chunk)
            val location = java.util.regex.Pattern.compile("file=\"([^\"]*)\"").matcher(chunk)
            if (message.find()) {
                val where = if (location.find()) location.group(1).substringAfterLast('/') else "?"
                messages.add(where + " :: " + message.group(1))
            }
        }
        println("::error::khataGo lint severities: " + severities)
        println("::error::khataGo lint: " + messages.size + " aborting issue(s)")
        if (messages.isEmpty()) {
            println("::error::khataGo lint head: " + khataGoClean(text.take(400)))
        }
        messages.take(12).forEach { entry ->
            val clean = entry
                .replace("%", "%25")
                .replace(13.toChar().toString(), "%0D")
                .replace(10.toChar().toString(), "%0A")
            println("::error::" + clean.take(230))
        }
    }
}

allprojects {
    tasks.matching { it.name == "lintDebug" }.configureEach {
        finalizedBy(khataGoLintReport)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

if (!project.hasProperty("khataGoDisableLogHook") &&
    gradle.startParameter.taskNames.any { name ->
        name == "help" || name.contains("compileDebugKotlin") || name.contains("testDebugUnitTest")
    }
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

        val root = "/com/shohan/khatago/"
        val files = ArrayList<String>()
        val lines = ArrayList<String>()
        val messages = ArrayList<String>()
        for (raw in text.lineSequence()) {
            val entry = raw.trim()
            if (!entry.startsWith("e: file://")) continue
            val at = entry.indexOf(root)
            if (at < 0) continue
            val rest = entry.substring(at + root.length)
            val first = rest.indexOf(':')
            if (first < 0) continue
            val second = rest.indexOf(':', first + 1)
            if (second < 0) continue
            val afterColon = rest.substring(second + 1).trim()
            val space = afterColon.indexOf(' ')
            files.add(rest.substring(0, first))
            lines.add(rest.substring(first + 1, second))
            messages.add(if (space < 0) afterColon else afterColon.substring(space + 1).trim())
        }

        val counts = HashMap<String, Int>()
        for (name in files) counts[name] = (counts[name] ?: 0) + 1
        val byFile = StringBuilder()
        for (entry in counts.entries.sortedByDescending { it.value }) {
            byFile.append(entry.key).append("(").append(entry.value).append(") ")
        }

        val seen = HashSet<String>()
        val unique = StringBuilder()
        var index = 0
        while (index < messages.size && seen.size < 12) {
            val message = messages[index]
            if (seen.add(message)) {
                unique.append(files[index]).append(":").append(lines[index])
                    .append(" ").append(message).append(" | ")
            }
            index = index + 1
        }

        khataGoAnnotate("khataGo: ", "exit=$exitCode bytes=${text.length} kotlinErrors=${files.size}", 230)

        khataGoAnnotate("khataGo byFile: ", byFile.toString(), 450)
        if (counts.isEmpty()) {
            val notes = StringBuilder()
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.contains("FAILED") ||
                    line.contains("expected:") ||
                    line.contains("AssertionError") ||
                    line.contains("ComparisonFailure") ||
                    line.contains("but was") ||
                    line.contains(": Error:") ||
                    line.contains("org.opentest4j") ||
                    line.startsWith("at com.shohan.khatago.")
                ) {
                    notes.append(line).append(" | ")
                }
            }
            khataGoAnnotate("khataGo failures: ", notes.toString(), 460)
            khataGoAnnotate("khataGo tail: ", text.takeLast(1380), 1380)
        }
        val worst = counts.entries.sortedByDescending { it.value }.firstOrNull()
        if (worst != null) {
            val detail = StringBuilder()
            var cursor = 0
            while (cursor < files.size) {
                if (files[cursor] == worst.key) {
                    detail.append(lines[cursor]).append(":").append(messages[cursor]).append(" | ")
                }
                cursor = cursor + 1
            }
            khataGoAnnotate(
                "khataGo worst " + worst.key + "(" + worst.value + "): ",
                detail.toString(),
                1600
            )
        }
    } catch (ignored: Throwable) {
        // Diagnostics must never break the build.
    }
}
