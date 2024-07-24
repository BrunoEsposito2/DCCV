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
            /* mkdir -p build &&
               cd build &&*/
            standardOutput = System.out
            errorOutput = System.err
        }
    }
}
