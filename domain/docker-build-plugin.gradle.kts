import org.gradle.internal.os.OperatingSystem

tasks.register("dockerBuild") {
    if (OperatingSystem.current().isWindows) {
        doFirst {
            exec {
                commandLine("cmd", "/c", "docker build -t ubuntu-opencv_build .")
                standardOutput = System.out
                errorOutput = System.err
            }
        }
        doLast {
            exec {
                commandLine(
                    "cmd", "/c",
                    "docker run -v %cd%\\\\..:/workspace -v /workspace/.gradle -v %cd%\\\\../.gradle:/tmp/.gradle --name ubuntu-opencv_build-container --rm ubuntu-opencv_build /bin/bash -c \"GRADLE_USER_HOME=/tmp/.gradle ./gradlew build\""
                )
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    } else {
        standardOutput = println("dockerBuild task will only be executed via github action")
    }
}
