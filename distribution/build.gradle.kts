import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import java.io.ByteArrayOutputStream

/*tasks.register("createJar") {
    dependsOn(project.subprojects.map { it.tasks.named("jar") })
    project.subprojects.forEach { p ->
        exec {
            commandLine("./gradlew", "${p.name}:jar")
        }
    }
}*/

tasks.register("deployPortainer") {
    exec {
        commandLine("docker", "stack", "deploy", "-c", "portainer.yml", "portainer")
    }
}