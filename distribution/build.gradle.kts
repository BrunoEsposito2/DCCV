import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import java.io.ByteArrayOutputStream

tasks.register("initSwarm") {
    doFirst {
        // Verifica se lo swarm è già inizializzato
        val swarmStatus = ByteArrayOutputStream()
        exec {
            commandLine("docker", "info", "--format", "{{.Swarm.LocalNodeState}}")
            standardOutput = swarmStatus
        }

        if (swarmStatus.toString().trim() != "active") {
            println("Initiating swarm...")
            // Inizializza lo swarm se non è attivo
            exec {
                commandLine("docker", "swarm", "init")
            }
        } else {
            println("Swarm is already active. Ongoing leave process...")
            exec {
                commandLine("docker", "swarm", "leave", "--force")
            }
            Thread.sleep(4000)
            println("Initiating swarm...")
            exec {
                commandLine("docker", "swarm", "init")
            }
        }
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

tasks.register("deploySwarm") {
    dependsOn(/*"createJar",*/ "initSwarm")
    doFirst {
        var joinToken = ""
        var managerIp = ""

        // Get the join token
        val tokenOutput = ByteArrayOutputStream()
        exec {
            commandLine("docker", "swarm", "join-token", "-q", "worker")
            standardOutput = tokenOutput
        }
        joinToken = tokenOutput.toString().trim()

        // Get the IP of the manager
        val ipOutput = ByteArrayOutputStream()
        exec {
            commandLine("docker", "node", "inspect", "--format", "{{.Status.Addr}}", "self")
            standardOutput = ipOutput
        }
        managerIp = ipOutput.toString().trim()

        // Create the overlay network if it does not already exist
        exec {
            commandLine("docker", "network", "create", "--driver", "overlay", "--attachable", "swarm-network")
            isIgnoreExitValue = true
        }

        // Deploy the cameranode
        exec {
            environment("JOIN_TOKEN", joinToken)
            environment("MANAGER_IP", managerIp)
            commandLine("docker-compose", "-f", "cameranode/docker-compose.yml", "up", "--build", "-d")
        }

        // Deploy the usernode
         exec {
            environment("JOIN_TOKEN", joinToken)
            environment("MANAGER_IP", managerIp)
            commandLine("docker-compose", "-f", "usernode/docker-compose.yml", "up", "--build", "-d")
        }

        // Deploy the utilitynode
        exec {
            environment("JOIN_TOKEN", joinToken)
            environment("MANAGER_IP", managerIp)
            commandLine("docker-compose", "-f", "utilitynode/docker-compose.yml", "up", "--build", "-d")
        }
    }

    //finalizedBy("deployPortainer")
}

tasks.register("deployPortainer") {
    exec {
        commandLine("docker", "stack", "deploy", "-c", "portainer.yml", "portainer")
    }
}

tasks.register<Test>("testSwarmDeployed") {
    shouldRunAfter("deploySwarm")
    doLast {
        // Ottieni i nomi dei container
        val containerNames = ByteArrayOutputStream()
        exec {
            commandLine("docker-compose", "ps", "--format", "{{.Name}}")
            standardOutput = containerNames
        }

        // Estrai i nomi dall'output
        val namesList = containerNames.toString().trim().split("\n")

        val nodeOutput = ByteArrayOutputStream()
        exec {
            commandLine("docker", "node", "ls")
            standardOutput = nodeOutput
        }
        println("Current swarm nodes: ${nodeOutput.toString().trim()}")

        // Verifica se i nodi sono stati aggiunti correttamente
        assertTrue((namesList.size + 1) == nodeOutput.toString().split("\n").size,
            "Not all nodes joined the current swarm. Please, fix it and try again.")

        println("All tests passed.")
    }
}