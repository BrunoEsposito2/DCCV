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
//    } else if (OperatingSystem.current().isLinux) {
//        doFirst {
//            exec {
//                commandLine("sh", "-c", "docker build -t ubuntu-opencv_build .")
//                standardOutput = System.out
//                errorOutput = System.err
//            }
//        }
//        doLast {
//            val mounts = project.projectDir.parentFile.listFiles()
//                ?.filter { x -> !(x.name == ".gradle" || x.name == ".idea") }
//                ?.map { "-v ${it.absolutePath}:/workspace/${it.name}" }
//                ?.map { it.replace(",", " ") }
//                ?.joinToString(" ")
//            println("MOUNTS: \n" + mounts)
//            exec {
//                commandLine(
//                    "sh", "-c", "docker run ${mounts} --name ubuntu-opencv_build-container --rm ubuntu-opencv_build /bin/bash -c './gradlew build'"
//                )
//                standardOutput = System.out
//                errorOutput = System.err
//            }
//        }
    } else if (OperatingSystem.current().isLinux) { //(System.getenv("GITHUB_ACTIONS") == "true") {
        exec {
            commandLine("sh", "-c", "./gradlew build")
        }
    }
}
