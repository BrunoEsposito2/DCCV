plugins {
    id("java")
    id("com.github.node-gradle.node") version "3.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:31.0.1-jre")
}

// Configura il task per installare le dipendenze npm
tasks.named<com.github.gradle.node.npm.task.NpmInstallTask>("npmInstall") {
    workingDir.set(file("interface"))
}

// Task per costruire l'app React
tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildReact") {
    dependsOn(tasks.named("npmInstall"))
    npmCommand.set(listOf("run", "build"))
    workingDir.set(file("interface"))
    inputs.dir("interface")
    outputs.dir("interface/build")
}

// Task per avviare il server di sviluppo React
tasks.register<com.github.gradle.node.npm.task.NpmTask>("runReact") {
    dependsOn(tasks.named("buildReact"))
    npmCommand.set(listOf("start"))
    workingDir.set(file("interface"))
    inputs.dir("interface")
    outputs.upToDateWhen { false } // Assicurarsi che il task non sia considerato up-to-date e venga eseguito sempre
}