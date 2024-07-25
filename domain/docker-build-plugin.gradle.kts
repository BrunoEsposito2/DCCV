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
    } else if (OperatingSystem.current().isLinux) {
        doFirst {
            exec {
                commandLine("sh", "-c", "docker build -t ubuntu-opencv_build .")
                standardOutput = System.out
                errorOutput = System.err
            }
        }
        doLast {
            exec {
                commandLine(
                    "sh", "-c", """
                        echo ${project.projectDir.parent} &&
                        echo "pwd stampato" &&
                        docker run -v ${project.projectDir.parent}:/workspace -v /workspace/.gradle -v ${project.projectDir.parent}/.gradle:/tmp/.gradle --name ubuntu-opencv_build-container --rm ubuntu-opencv_build /bin/bash -c 'GRADLE_USER_HOME=/tmp/.gradle ./gradlew build'
                    """)
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}