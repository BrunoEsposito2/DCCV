import org.gradle.internal.os.OperatingSystem

tasks.register("buildCMake") {
    onlyIf { OperatingSystem.current().isLinux }
    doLast {
        exec {
            workingDir = buildDir
            commandLine("sh", "-c", """
                        cmake .. &&
                        cmake --build . &&
                        cmake --install . --prefix=${project.buildDir}/install
                       """)
            standardOutput = System.out
            errorOutput = System.err
        }
    }
}
