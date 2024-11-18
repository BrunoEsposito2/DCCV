import org.gradle.internal.os.OperatingSystem

tasks.register("dockerBuild") {
    if (OperatingSystem.current().isWindows) {
        doFirst {
            exec {
                commandLine("cmd", "/c", "docker build -t ubuntu-opencv_build_streaming .")
                standardOutput = System.out
                errorOutput = System.err
            }
        }
        doLast {
            exec {
                commandLine(
                    "cmd", "/c",
                    "docker run -p 5555:5555 --name ubuntu-opencv_build-container" +
                            " --rm ubuntu-opencv_build_streaming /bin/bash " +
                            "-c \"gradle build\""
                )
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}
