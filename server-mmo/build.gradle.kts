plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pl.decodesoft"
version = "1.0"

repositories {
    mavenCentral()
}

val gdxVersion = "1.13.5"
val ktorVersion = "3.2.1"
val postgresql = "42.7.7"
val logbackVersion = "1.5.18"

dependencies {
    // Twoje istniejące zależności
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Zależności do serwera
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    // Logowanie, Rejestracja
    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    // Exposed ORM
    implementation("org.postgresql:postgresql:$postgresql")
    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-json:0.44.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Connection pooling (opcjonalne, ale zalecane)
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Zależności LibGDX (jeśli potrzebne)
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("pl.decodesoft.MainKt")
}

// Konfiguracja Shadow JAR
tasks.shadowJar {
    archiveBaseName.set("server-mmo")
    archiveVersion.set("1.0")
    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = "pl.decodesoft.MainKt"
    }
}

// Zadanie do uruchomiania serwera w trybie development
tasks.register<JavaExec>("runServer") {
    group = "application"
    mainClass.set("pl.decodesoft.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

kotlin {
    jvmToolchain(21)
}