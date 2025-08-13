import org.gradle.internal.os.OperatingSystem

tasks.register("dockerBuild") {
    doFirst {
        delete("${projectDir}/build")
        if (OperatingSystem.current().isWindows) {
            exec {
                commandLine("cmd", "/c", "docker build -t ubuntu-opencv_build_streaming -f ../domain/Dockerfile ../")
                standardOutput = System.out
                errorOutput = System.err
            }
        } else {
            exec {
                commandLine("sh", "-c", "docker build -t ubuntu-opencv_build_streaming -f ../domain/Dockerfile ../")
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
    doLast {
        if (OperatingSystem.current().isWindows) {
            exec {
                commandLine(
                    "cmd", "/c",
                    "docker run" +
                            " -v " + projectDir + "/build:/DCCV/domain/build " + 
                            " -p 5555:5555 --name ubuntu-opencv_build-container " +
                            " --rm ubuntu-opencv_build_streaming /bin/bash " +
                            "-c \"gradle build -x test -x spotlessCheck -x spotlessApply && domain/run.sh\""
                )
                standardOutput = System.out
                errorOutput = System.err
            }
        } else {
            
            exec {
                commandLine(
                    "sh", "-c",
                    "docker run" +
                            " -v " + projectDir + "/domain/build:/DCCV/build " +
                            " -p 5555:5555 --name ubuntu-opencv_build-container " +
                            " --rm ubuntu-opencv_build_streaming /bin/bash " + 
                            "-c \"gradle build -x test -x spotlessCheck -x spotlessApply && domain/run.sh\""
                )
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}