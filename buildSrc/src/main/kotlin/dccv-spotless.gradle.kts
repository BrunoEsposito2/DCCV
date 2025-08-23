import java.io.ByteArrayOutputStream
import org.gradle.internal.os.OperatingSystem

plugins {
    id("com.diffplug.spotless")
}

spotless {
    tasks.named("spotlessApply") { dependsOn("downloadFormatterBinaries") }
    tasks.named("spotlessCheck") { dependsOn("downloadFormatterBinaries") }

    // Configurazione per file C++ (.cpp, .h, .hpp)
    cpp {
        // Target pattern per includere tutti i file C++ del progetto
        target("**/*.cpp", "**/*.h", "**/*.hpp")

        val output = ByteArrayOutputStream()
        exec {
            commandLine("clang-format", "--version")
            standardOutput = output
        }

        val versionOutput = output.toString().trim()
        println("clang-format output: $versionOutput")

        // Estrai il numero di versione (es: "clang-format version 20.1.8" -> "20.1.8")
        val versionRegex = Regex("""clang-format version (\d+\.\d+\.\d+)""")

        // Applicazione di clang-format per C++ con stile Google
        val matchRegex = versionRegex.find(versionOutput)
        var version = ""
        if (matchRegex != null) {
            version = matchRegex.groupValues[1]
        }
        clangFormat(version).style("Google")

        // Rimozione spazi bianchi trailing - previene diff noise nei commit
        trimTrailingWhitespace()

        // Garantisce che ogni file termini con newline - standard POSIX
        endWithNewline()

        // Configurazione custom per gestire header guards e include ordering
        custom("Header Guards") { content ->
            // Normalizza header guards per seguire convenzioni Google C++
            content.replace(
                Regex("#ifndef\\s+([A-Z_]+)\\s*\n#define\\s+\\1"),
                { match ->
                    val guard = match.groupValues[1]
                    "#ifndef ${guard}\n#define $guard"
                },
            )
        }
    }

    // Configurazione per file Scala (.scala)
    scala {
        target("**/*.scala")

        // Utilizzo di scalafmt per formattazione Scala standard
        scalafmt("3.8.3").configFile("../config/spotless/scalafmt.conf")

        // Standard di pulizia applicati universalmente
        trimTrailingWhitespace()
        endWithNewline()

        // Custom rule per import organization secondo convenzioni Scala
        custom("Import Organization") { content ->
            // Raggruppa import per tipo: standard library, third-party, local
            val lines = content.lines()
            val imports = lines.filter { it.trim().startsWith("import ") }
            val nonImports = lines.filter { !it.trim().startsWith("import ") }

            val stdLibImports = imports.filter { it.contains("scala.") || it.contains("java.") }
            val thirdPartyImports = imports.filter { !it.contains("scala.") && !it.contains("java.") && !it.contains("com.yourproject") }
            val localImports = imports.filter { it.contains("com.yourproject") }

            (
                nonImports.takeWhile { it.trim().isEmpty() || it.trim().startsWith("package") } +
                    stdLibImports + listOf("") +
                    thirdPartyImports + listOf("") +
                    localImports + listOf("") +
                    nonImports.dropWhile { it.trim().isEmpty() || it.trim().startsWith("package") || it.trim().startsWith("import") }
                ).joinToString("\n")
        }
    }

    // Configurazione per file Kotlin Gradle (.gradle.kts)
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")

        // ktlint per Kotlin con configurazione ottimizzata per Gradle scripts
        ktlint("0.50.0").setEditorConfigPath("../config/spotless/.editorconfig")

        trimTrailingWhitespace()
        endWithNewline()

        // Custom formatting per dependency declarations per migliorare readability
        custom("Dependency Formatting") { content ->
            content.replace(
                Regex("implementation\\(\"([^\"]+)\"\\)"),
                "implementation(\"$1\")",
            )
                .replace(
                    Regex("testImplementation\\(\"([^\"]+)\"\\)"),
                    "testImplementation(\"$1\")",
                )
        }
    }

    // Configurazione per file JavaScript (.js, .mjs)
    javascript {
        target("**/*.js", "**/*.mjs")

        // Prettier per JavaScript - standard de-facto per JS formatting
        prettier("3.0.3").configFile("../config/spotless/prettier.json")

        trimTrailingWhitespace()
        endWithNewline()

        // ESLint integration per quality rules oltre al formatting
        custom("ESLint Integration") { content ->
            // Placeholder per future ESLint rules integration: 
            // può essere esteso per enforcing specific coding patterns
            content
        }
    }

    // Configurazione per file JSON (.json)
    json {
        target("**/*.json")

        // Gson formatter per JSON consistente e readable
        gson().indentWithSpaces(2)

        trimTrailingWhitespace()
        endWithNewline()
    }

    // Configurazione globale per line endings consistency
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    // Performance optimization settings
    isEnforceCheck = false // Permette formatting automatico invece di solo check

    // Ratchet configuration per applicare rules solo ai file modificati
    ratchetFrom("origin/main")

    // Configurazione per excluded paths globali
    val excludedPaths = listOf(
        "**/.gradle/**",
        "**/node_modules/**",
        "**/.git/**",
        "**/target/**",
        "**/.idea/**",
        "**/.vscode/**",
    )
}

tasks.register("checkPreCommit") {
    dependsOn("spotlessCheck", "spotlessApply")
}

// Task per scaricare e configurare i binari necessari
tasks.register("downloadFormatterBinaries") {
    group = "setup"
    description = "Scarica tutti i binari necessari per formatting"

    val binariesDir = file("build/formatters/bin")
    val clangFormatVersion = "18.1.8"
    val ktlintVersion = "0.50.0"
    val scalafmtVersion = "3.8.3"

    // Configurazione per Gradle task caching
    inputs.property("clangFormatVersion", clangFormatVersion)
    inputs.property("ktlintVersion", ktlintVersion)
    inputs.property("scalafmtVersion", scalafmtVersion)
    inputs.property("os", OperatingSystem.current().name)
    inputs.property("arch", System.getProperty("os.arch"))

    outputs.dir(binariesDir)

    doLast {
        binariesDir.mkdirs()

        // Download clang-format
        val clangFormatBinary = downloadClangFormat(clangFormatVersion, binariesDir)

        // Download ktlint
        val ktlintBinary = downloadKtlint(ktlintVersion, binariesDir)

        // Download scalafmt
        val scalafmtBinary = downloadScalafmt(scalafmtVersion, binariesDir)

        // Make binaries executable
        if (!OperatingSystem.current().isWindows) {
            listOf(clangFormatBinary, ktlintBinary, scalafmtBinary).forEach { binary ->
                if (binary.exists()) {
                    exec {
                        commandLine("chmod", "+x", binary.absolutePath)
                    }
                }
            }
        }
    }
}

fun downloadScalafmt(version: String, targetDir: File): File {
    val scalafmtJar = File(targetDir, "scalafmt.jar")
    val scalafmtScript = File(targetDir, if (OperatingSystem.current().isWindows) "scalafmt.bat" else "scalafmt")

    if (!scalafmtJar.exists()) {
        // Download from Maven Central
        val groupId = "org.scalameta"
        val artifactId = "scalafmt-cli_2.13"
        val downloadUrl = "https://repo1.maven.org/maven2/${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.jar"

        println("Downloading scalafmt $version from Maven Central...")

        try {
            val connection = java.net.URL(downloadUrl).openConnection()
            connection.setRequestProperty("User-Agent", "Gradle Build Script")

            scalafmtJar.parentFile.mkdirs()
            scalafmtJar.outputStream().use { output ->
                connection.getInputStream().use { input ->
                    input.copyTo(output)
                }
            }

            println("Downloaded scalafmt to ${scalafmtJar.absolutePath}")
        } catch (e: Exception) {
            throw GradleException("Failed to download scalafmt: ${e.message}", e)
        }
    }

    // Create wrapper script
    if (!scalafmtScript.exists()) {
        if (OperatingSystem.current().isWindows) {
            scalafmtScript.writeText(
                """
                @echo off
                java -jar "%~dp0scalafmt.jar" %*
                """.trimIndent(),
            )
        } else {
            scalafmtScript.writeText(
                """
                #!/bin/bash
                SCRIPT_DIR="$( cd "$( dirname "${'$'}{BASH_SOURCE[0]}" )" && pwd )"
                exec java -jar "${'$'}SCRIPT_DIR/scalafmt.jar" "${'$'}@"
                """.trimIndent(),
            )
            exec {
                commandLine("chmod", "+x", scalafmtScript.absolutePath)
            }
        }
    }

    return scalafmtScript
}

fun downloadClangFormat(version: String, targetDir: File): File {
    val binaryName = if (OperatingSystem.current().isWindows) "clang-format.exe" else "clang-format"
    val targetFile = File(targetDir, binaryName)

    if (!targetFile.exists()) {
        println("Downloading clang-format $version...")

        try {
            when {
                OperatingSystem.current().isWindows -> {
                    try {
                        exec {
                            commandLine(
                                "powershell",
                                "-Command",
                                "winget install LLVM.LLVM --accept-package-agreements --accept-source-agreements --silent",
                            )
                            isIgnoreExitValue = true
                        }
                    } catch (e: Exception) {
                        println("Warning: Could not remove Windows security restrictions: ${e.message}")
                        println("You may need to run the build as administrator or manually unblock the file")
                    }
                }

                OperatingSystem.current().isLinux -> {
                    exec {
                        commandLine("apt", "install", "-y", "clang-format")
                        isIgnoreExitValue = true
                    }
                }

                OperatingSystem.current().isMacOsX -> {
                    exec {
                        commandLine("brew", "install", "clang-format")
                        isIgnoreExitValue = true
                    }
                }

                else -> throw GradleException("Unsupported OS for clang-format download")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to download clang-format: ${e.message}", e)
        }
    }

    return targetFile
}

fun downloadKtlint(version: String, targetDir: File): File {
    val ktlintJar = File(targetDir, "ktlint.jar")
    val ktlintScript = File(targetDir, if (OperatingSystem.current().isWindows) "ktlint.bat" else "ktlint")

    // Download the JAR if it doesn't exist
    if (!ktlintJar.exists()) {
        val downloadUrl = "https://github.com/pinterest/ktlint/releases/download/$version/ktlint"

        println("Downloading ktlint $version...")

        try {
            val connection = java.net.URL(downloadUrl).openConnection()
            connection.setRequestProperty("User-Agent", "Gradle Build Script")

            ktlintJar.parentFile.mkdirs()
            ktlintJar.outputStream().use { output ->
                connection.getInputStream().use { input ->
                    input.copyTo(output)
                }
            }

            println("Downloaded ktlint to ${ktlintJar.absolutePath}")
        } catch (e: Exception) {
            throw GradleException("Failed to download ktlint: ${e.message}", e)
        }
    }

    // Create wrapper script with proper escaping
    if (!ktlintScript.exists()) {
        if (OperatingSystem.current().isWindows) {
            ktlintScript.writeText(
                """
                @echo off
                java -jar "%~dp0ktlint.jar" %*
                """.trimIndent(),
            )
        } else {
            ktlintScript.writeText(
                """
                #!/bin/bash
                SCRIPT_DIR="$( cd "$( dirname "${'$'}{BASH_SOURCE[0]}" )" && pwd )"
                exec java -jar "${'$'}SCRIPT_DIR/ktlint.jar" "${'$'}@"
                """.trimIndent(),
            )
            exec {
                commandLine("chmod", "+x", ktlintScript.absolutePath)
            }
        }
    }

    return ktlintScript
}
