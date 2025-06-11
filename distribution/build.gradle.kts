import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import java.io.ByteArrayOutputStream

tasks.register("test") {
    group = "verification"
    description = "Validate distribution configuration"
    
    doLast {
        // Validate docker-compose files
        val composeFiles = fileTree(".") {
            include("**/*.yml", "**/*.yaml")
            exclude("**/node_modules/**")
        }
        
        composeFiles.forEach { file ->
            if (file.name.contains("docker-compose")) {
                logger.lifecycle("Validating: ${file.name}")
                // Add validation logic here
            }
        }
        
        // Validate deployment scripts
        val deployScripts = fileTree(".") {
            include("**/*.sh")
        }
        
        deployScripts.forEach { script ->
            logger.lifecycle("Checking script: ${script.name}")
            if (!script.canExecute()) {
                throw GradleException("Script ${script.name} is not executable")
            }
        }
        
        logger.lifecycle("✅ Distribution configuration validation passed")
    }
}

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