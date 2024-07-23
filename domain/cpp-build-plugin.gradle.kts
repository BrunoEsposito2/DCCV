import org.gradle.internal.os.OperatingSystem

tasks.register("buildCMake") {
    val buildDir = file("build")
    buildDir.mkdirs()

    doLast {
        if (OperatingSystem.current().isWindows) {
            exec {
                workingDir = buildDir
                commandLine("cmd", "/c", "cmake", "..")
                standardOutput = System.out
                errorOutput = System.err
            }
            exec {
                workingDir = buildDir
                commandLine("cmd", "/c", "cmake", "--build", ".")
                standardOutput = System.out
                errorOutput = System.err
            }
        } else if (OperatingSystem.current().isLinux) {
            exec {
                workingDir = buildDir
                commandLine("sh", "-c", "cmake", "..")
                standardOutput = System.out
                errorOutput = System.err
            }
            exec {
                workingDir = buildDir
                commandLine("sh", "-c", "cmake", "--build", ".")
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}
