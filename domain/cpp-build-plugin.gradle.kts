import org.gradle.internal.os.OperatingSystem

tasks.register("buildCMake") {
    val buildDir = file("build")
    buildDir.mkdirs()

    doLast {
        if (OperatingSystem.current().isLinux) {
            exec {
                workingDir = buildDir
                commandLine("sh", "-c", """
                            cmake .. &&
                            cmake --build .
                            """)
                /* mkdir -p build &&
                   cd build &&*/
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}
