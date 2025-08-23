import org.gradle.internal.os.OperatingSystem
import java.io.ByteArrayOutputStream

plugins {
    // Apply the cpp-application plugin to add support for building C++ executables
    `cpp-application`

    // Apply the cpp-unit-test plugin to add support for building and running C++ test executables
    `cpp-unit-test`
}

// Configurazione specifica per C++
application {
    targetMachines.add(machines.linux.x86_64)
    targetMachines.add(machines.windows.x86_64)
}

unitTest {
    targetMachines.add(machines.linux.x86_64)
    targetMachines.add(machines.windows.x86_64)
}

val copyOpenCVLibs by tasks.registering(Copy::class) {
    onlyIf { OperatingSystem.current().isLinux }
    val libDir = file("build/libs")
    libDir.mkdirs()
    from("/usr/local/lib")
    into("build/libs")
    include("libopencv_*.so*")
}

val createDistribution by tasks.registering(Sync::class) {
    onlyIf { OperatingSystem.current().isLinux }
    from("build/exe/main/debug/linux") { into("bin") }
    from(tasks.named("copyOpenCVLibs")) { into("libs") }

    from("build/exe/main/debug/linux")
    from("build/libs")
    from("build/install/bin") { into("bin") }>
    from("run.sh")
    into("build/release/domain")
}

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

// Configura cpp-application per usare l'output di CMake
tasks.withType<LinkExecutable>().configureEach {
    onlyIf { OperatingSystem.current().isLinux }
    dependsOn(copyOpenCVLibs, createDistribution, "buildCMake")
    linkerArgs.addAll(listOf(
        "-L/usr/local/lib",
        "-lopencv_core",
        "-lopencv_imgproc",
        "-lopencv_objdetect",
        "-lopencv_highgui",
        "-lopencv_imgcodecs",
        "-lopencv_videoio",
        "-lopencv_calib3d",
        "-lopencv_features2d",
        "-lopencv_video"
    ))
    linkerArgs.add("-Wl,-rpath,/usr/local/lib")
    linkerArgs.add("-Wl,-rpath,\$ORIGIN/libs")
}

tasks.withType<CppCompile>().configureEach {
    dependsOn("buildCMake")
    onlyIf { OperatingSystem.current().isLinux }
    includes.from("/usr/local/include/opencv4")
}
